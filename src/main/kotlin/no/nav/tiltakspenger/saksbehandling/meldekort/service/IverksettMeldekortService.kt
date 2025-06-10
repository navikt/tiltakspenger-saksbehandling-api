package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.ports.OppgaveKlient
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkStønadRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.IverksettMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeIverksetteMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.BrukersMeldekortRepo
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
    val brukersMeldekortRepo: BrukersMeldekortRepo,
    val sessionFactory: SessionFactory,
    private val utbetalingsvedtakRepo: UtbetalingsvedtakRepo,
    private val statistikkStønadRepo: StatistikkStønadRepo,
    private val clock: Clock,
    private val oppgaveKlient: OppgaveKlient,
) {
    private val log = KotlinLogging.logger {}

    suspend fun iverksettMeldekort(
        kommando: IverksettMeldekortKommando,
    ): Either<KanIkkeIverksetteMeldekort, Pair<Sak, Utbetalingsvedtak>> {
        krevBeslutterRolle(kommando.beslutter)

        val meldekortId = kommando.meldekortId
        val sakId = kommando.sakId
        // Denne sjekker at saksbehandler har tilgang til personen og at den har en av rollene SAKSBEHANDLER eller BESLUTTER
        val sak = sakService.sjekkTilgangOgHentForSakId(sakId, kommando.beslutter, kommando.correlationId)
        val meldekortBehandling: MeldekortBehandling = sak.hentMeldekortBehandling(meldekortId)
            ?: throw IllegalArgumentException("Fant ikke meldekort med id $meldekortId i sak $sakId")

        require(meldekortBehandling is MeldekortBehandletManuelt) {
            "Meldekortet må være behandlet for å iverksettes"
        }
        require(meldekortBehandling.beslutter != null && meldekortBehandling.status == MeldekortBehandlingStatus.UNDER_BESLUTNING) {
            return KanIkkeIverksetteMeldekort.BehandlingenErIkkeUnderBeslutning.left()
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
            ferdigstillOppgave(meldeperiode.id, meldekortId)
            sak.oppdaterMeldekortbehandling(iverksattMeldekortbehandling)
                .leggTilUtbetalingsvedtak(utbetalingsvedtak) to utbetalingsvedtak
        }
    }

    private suspend fun ferdigstillOppgave(
        meldeperiodeId: MeldeperiodeId,
        meldekortId: MeldekortId,
    ) {
        val brukersMeldekort = brukersMeldekortRepo.hentForMeldeperiodeId(meldeperiodeId)
        brukersMeldekort?.oppgaveId?.let { id ->
            log.info { "Ferdigstiller oppgave med id $id for meldekort med meldekortId $meldekortId" }
            oppgaveKlient.ferdigstillOppgave(id)
        }
    }
}
