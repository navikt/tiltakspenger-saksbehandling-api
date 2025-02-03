package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.felles.exceptions.TilgangException
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.meldekort.domene.IverksettMeldekortKommando
import no.nav.tiltakspenger.meldekort.domene.KanIkkeIverksetteMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.opprettNesteMeldeperiode
import no.nav.tiltakspenger.meldekort.ports.MeldekortBehandlingRepo
import no.nav.tiltakspenger.meldekort.ports.MeldeperiodeRepo
import no.nav.tiltakspenger.saksbehandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.service.person.PersonService
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService
import no.nav.tiltakspenger.utbetaling.domene.opprettUtbetalingsvedtak
import no.nav.tiltakspenger.utbetaling.domene.tilStatistikk
import no.nav.tiltakspenger.utbetaling.ports.UtbetalingsvedtakRepo
import java.lang.IllegalStateException

class IverksettMeldekortService(
    val sakService: SakService,
    val meldekortBehandlingRepo: MeldekortBehandlingRepo,
    val meldeperiodeRepo: MeldeperiodeRepo,
    val sessionFactory: SessionFactory,
    private val tilgangsstyringService: TilgangsstyringService,
    private val personService: PersonService,
    private val utbetalingsvedtakRepo: UtbetalingsvedtakRepo,
    private val statistikkStønadRepo: StatistikkStønadRepo,
) {
    private val log = KotlinLogging.logger {}

    suspend fun iverksettMeldekort(
        kommando: IverksettMeldekortKommando,
    ): Either<KanIkkeIverksetteMeldekort, MeldekortBehandling.MeldekortBehandlet> {
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
        meldekortBehandling as MeldekortBehandling.MeldekortBehandlet
        require(meldekortBehandling.beslutter == null && meldekortBehandling.status == MeldekortBehandlingStatus.KLAR_TIL_BESLUTNING) {
            "Meldekort $meldekortId er allerede iverksatt"
        }
        val meldeperiode = meldekortBehandling.meldeperiode
        if (!sak.erSisteVersjonAvMeldeperiode(meldeperiode)) {
            throw IllegalStateException("Kan ikke iverksette meldekortbehandling hvor meldeperioden (${meldeperiode.versjon}) ikke er siste versjon av meldeperioden i saken. sakId: $sakId, meldekortId: $meldekortId")
        }

        val nesteMeldeperiode: Meldeperiode? = sak.opprettNesteMeldeperiode()?.let {
            if (meldekortBehandling.periode.tilOgMed.plusDays(1) != it.periode.fraOgMed) {
                log.info { "Neste meldeperiode (${it.periode}) er ikke sammenhengende med det vedtatte meldekortet sin meldeperiode (${meldekortBehandling.periode}). Oppretter ikke ny meldeperiode. behandlingId: ${meldekortBehandling.id}, sakId: ${meldekortBehandling.sakId}, saksnummer: ${meldekortBehandling.saksnummer}" }
                null
            } else {
                it
            }
        }

        return meldekortBehandling.iverksettMeldekort(kommando.beslutter).onRight { iverksattMeldekort ->
            val eksisterendeUtbetalingsvedtak = sak.utbetalinger
            val utbetalingsvedtak = iverksattMeldekort.opprettUtbetalingsvedtak(
                saksnummer = sak.saksnummer,
                fnr = sak.fnr,
                eksisterendeUtbetalingsvedtak.lastOrNull()?.id,
            )
            val utbetalingsstatistikk = utbetalingsvedtak.tilStatistikk()

            sessionFactory.withTransactionContext { tx ->
                meldekortBehandlingRepo.oppdater(iverksattMeldekort, tx)
                // TODO John og Anders: På et tidspunkt bør vi kanskje flytte generering av meldeperioder ut i en jobb?
                nesteMeldeperiode?.also { meldeperiodeRepo.lagre(it, tx) }
                utbetalingsvedtakRepo.lagre(utbetalingsvedtak, tx)
                statistikkStønadRepo.lagre(utbetalingsstatistikk, tx)
            }
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
