package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.Utbetalingskontroll
import no.nav.tiltakspenger.saksbehandling.beregning.beregnMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService
import java.time.Clock

class OppdaterBeregningOgSimuleringMeldekortService(
    val sakService: SakService,
    val meldekortbehandlingRepo: MeldekortbehandlingRepo,
    val simulerService: SimulerService,
    val sessionFactory: SessionFactory,
    val clock: Clock,
) {
    /**
     * Oppdaterer simuleringen av utbetaling på en åpen meldekortbehandling som er under behandling.
     */
    suspend fun oppdaterSimulering(
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler,
    ): Either<KunneIkkeSimulere, Pair<Sak, Meldekortbehandling>> {
        val sak: Sak = sakService.hentForSakId(sakId)
        val meldekortbehandling: Meldekortbehandling = sak.hentMeldekortbehandling(meldekortId)!!

        require(saksbehandler.navIdent == meldekortbehandling.saksbehandler) {
            "Kan kun oppdatere simulering på en behandling dersom saksbehandler som ber om det er den samme som er satt på behandlingen"
        }

        val simuleringMedMetadata: SimuleringMedMetadata = simulerService.simulerMeldekort(
            behandling = meldekortbehandling,
            forrigeUtbetaling = sak.utbetalinger.lastOrNull(),
            meldeperiodeKjeder = sak.meldeperiodeKjeder,
            brukersNavkontor = { meldekortbehandling.navkontor },
            kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort,
        ).getOrElse { return it.left() }

        val oppdatertMeldekortbehandling = meldekortbehandling.oppdaterSimulering(simuleringMedMetadata.simulering)
        val oppdatertSak = sak.oppdaterMeldekortbehandling(oppdatertMeldekortbehandling)

        sessionFactory.withTransactionContext { tx ->
            meldekortbehandlingRepo.oppdater(oppdatertMeldekortbehandling, simuleringMedMetadata, tx)
        }

        return (oppdatertSak to oppdatertMeldekortbehandling).right()
    }

    /**
     * Beregner og simulerer meldekortbehandlingen på nytt slik saken ser ut nå, og setter resultatet som [Utbetalingskontroll] på behandlingen.
     * Kjøres når behandlingen sendes videre i flyten, altså til beslutter og ved iverksettelse.
     *
     * Flere meldekortbehandlinger kan være åpne på samme sak samtidig, og de påvirker hverandre gjennom de vedtatte meldeperiodeberegningene.
     * Blir en annen behandling iverksatt mellom saksbehandlers simulering og neste steg, er tallene saksbehandler så på ikke lenger de som ville blitt utbetalt.
     * [no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling] sammenligner kontrollen med simuleringen på behandlingen og stopper flyten ved avvik.
     *
     * Behandlingen persisteres ikke her; det er kallerens ansvar.
     */
    suspend fun oppdaterUtbetalingskontroll(
        sak: Sak,
        meldekortId: MeldekortId,
        saksbehandlerEllerBeslutter: Saksbehandler,
    ): Either<KunneIkkeSimulere, Pair<Sak, Meldekortbehandling>> {
        val behandling = sak.hentMeldekortbehandling(meldekortId)!!

        val beregningOgSimulering = sak.beregnOgSimuler(
            behandling = behandling,
            saksbehandlerEllerBeslutter = saksbehandlerEllerBeslutter,
        ).getOrElse { return it.left() }

        val utbetalingskontroll = Utbetalingskontroll(
            beregning = beregningOgSimulering.first,
            simulering = beregningOgSimulering.second.simulering,
        )

        val oppdatertBehandling = behandling.oppdaterUtbetalingskontroll(
            oppdatertKontroll = utbetalingskontroll,
            clock = clock,
        )
        val oppdatertSak = sak.oppdaterMeldekortbehandling(oppdatertBehandling)

        return (oppdatertSak to oppdatertBehandling).right()
    }

    private suspend fun Sak.beregnOgSimuler(
        behandling: Meldekortbehandling,
        saksbehandlerEllerBeslutter: Saksbehandler,
    ): Either<KunneIkkeSimulere, Pair<Beregning, SimuleringMedMetadata>> {
        when (behandling.status) {
            MeldekortbehandlingStatus.UNDER_BEHANDLING -> require(saksbehandlerEllerBeslutter.navIdent == behandling.saksbehandler) {
                "Kan kun oppdatere utbetalingskontroll på en meldekortbehandling dersom saksbehandler som ber om det er den samme som er satt på behandlingen"
            }

            MeldekortbehandlingStatus.UNDER_BESLUTNING -> require(saksbehandlerEllerBeslutter.navIdent == behandling.beslutter) {
                "Kan kun oppdatere utbetalingskontroll på en meldekortbehandling dersom beslutter som ber om det er den samme som er satt på behandlingen"
            }

            else -> throw IllegalStateException("Meldekortbehandling må være under behandling eller beslutning for at utbetalingskontrollen skal kunne oppdateres. Status er ${behandling.status}, sakId: ${behandling.sakId}, id: ${behandling.id}")
        }

        val beregning = this.beregnMeldekort(
            meldekortIdSomBeregnes = behandling.id,
            meldeperioderSomBeregnes = behandling.meldeperioder.meldeperioder.map { it.dager },
            beregningstidspunkt = nå(clock),
        )

        val simulering: SimuleringMedMetadata = simulerService.simulerMeldekort(
            behandling = behandling,
            forrigeUtbetaling = this.utbetalinger.lastOrNull(),
            meldeperiodeKjeder = this.meldeperiodeKjeder,
            kanSendeInnHelgForMeldekort = this.kanSendeInnHelgForMeldekort,
            beregning = beregning,
            brukersNavkontor = { behandling.navkontor },
        ).getOrElse { return it.left() }

        return (beregning to simulering).right()
    }
}
