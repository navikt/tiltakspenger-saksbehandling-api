package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveGateway
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.IverksettMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeIverksetteMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.opprettUtbetalingsvedtak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.tilStatistikk
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo
import java.time.Clock

class IverksettMeldekortService(
    val sakService: SakService,
    val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    val meldeperiodeRepo: MeldeperiodeRepo,
    val sessionFactory: SessionFactory,
    private val tilgangsstyringService: TilgangsstyringService,
    private val personService: PersonService,
    private val utbetalingsvedtakRepo: UtbetalingsvedtakRepo,
    private val statistikkStønadRepo: StatistikkStønadRepo,
    private val clock: Clock,
    private val oppgaveGateway: OppgaveGateway,
) {
    private val log = KotlinLogging.logger {}

    suspend fun iverksettMeldekort(
        kommando: IverksettMeldekortKommando,
    ): Either<KanIkkeIverksetteMeldekort, Pair<Sak, Utbetalingsvedtak>> {
        if (!kommando.beslutter.erBeslutter()) {
            return KanIkkeIverksetteMeldekort.MåVæreBeslutter(kommando.beslutter.roller).left()
        }

        val meldekortId = kommando.meldekortId
        val sakId = kommando.sakId
        kastHvisIkkeTilgangTilPerson(kommando.beslutter, meldekortId, kommando.correlationId)

        val sak = sakService.hentForSakId(sakId, kommando.beslutter, kommando.correlationId)
            .getOrElse { return KanIkkeIverksetteMeldekort.KunneIkkeHenteSak(it).left() }
        val meldekortBehandling: MeldekortBehandling = sak.hentMeldekortBehandling(meldekortId)
            ?: throw IllegalArgumentException("Fant ikke meldekort med id $meldekortId i sak $sakId")

        require(meldekortBehandling is MeldekortBehandletManuelt) {
            "Meldekortet må være behandlet for å iverksettes"
        }
        require(meldekortBehandling.beslutter == null && meldekortBehandling.status == MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING) {
            "Meldekort $meldekortId er allerede iverksatt"
        }

        val meldeperiode = meldekortBehandling.meldeperiode
        check(sak.erSisteVersjonAvMeldeperiode(meldeperiode)) {
            "Kan ikke iverksette meldekortbehandling hvor meldeperioden (${meldeperiode.versjon}) ikke er siste versjon av meldeperioden i saken. sakId: $sakId, meldekortId: $meldekortId"
        }

        return meldekortBehandling.iverksettMeldekort(kommando.beslutter, clock).map { iverksattMeldekortbehandling ->
            val eksisterendeUtbetalingsvedtak = sak.utbetalinger
            val utbetalingsvedtak = iverksattMeldekortbehandling.opprettUtbetalingsvedtak(
                saksnummer = sak.saksnummer,
                fnr = sak.fnr,
                forrigeUtbetalingsvedtak = eksisterendeUtbetalingsvedtak.lastOrNull(),
                clock = clock,
            )
            val utbetalingsstatistikk = utbetalingsvedtak.tilStatistikk()

            sessionFactory.withTransactionContext { tx ->
                meldekortBehandlingRepo.oppdater(iverksattMeldekortbehandling, tx)
                utbetalingsvedtakRepo.lagre(utbetalingsvedtak, tx)
                statistikkStønadRepo.lagre(utbetalingsstatistikk, tx)
            }
            meldekortBehandling.brukersMeldekort?.oppgaveId?.let { id ->
                log.info { "Ferdigstiller oppgave med id $id for meldekort med meldekortbehandling-id ${meldekortBehandling.id}" }
                oppgaveGateway.ferdigstillOppgave(id)
            }
            sak.oppdaterMeldekortbehandling(iverksattMeldekortbehandling)
                .leggTilUtbetalingsvedtak(utbetalingsvedtak) to utbetalingsvedtak
        }
    }

    private suspend fun kastHvisIkkeTilgangTilPerson(
        saksbehandler: Saksbehandler,
        meldekortId: MeldekortId,
        correlationId: CorrelationId,
    ) {
        val fnr = personService.hentFnrForMeldekortId(meldekortId)
        tilgangsstyringService
            .harTilgangTilPerson(
                fnr = fnr,
                roller = saksbehandler.roller,
                correlationId = correlationId,
            ).onLeft {
                throw IkkeFunnetException("Feil ved sjekk av tilgang til person. meldekortId: $meldekortId. CorrelationId: $correlationId")
            }.onRight {
                if (!it) throw TilgangException("Saksbehandler ${saksbehandler.navIdent} har ikke tilgang til person")
            }
    }
}
