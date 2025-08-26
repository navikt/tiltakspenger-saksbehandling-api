package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.beregning.BehandlingBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.Utbetalingsklient

class SimulerService(
    private val utbetalingsklient: Utbetalingsklient,
    private val navkontorService: NavkontorService,
    private val utbetalingRepo: UtbetalingRepo,
) {
    /**
     * Skal kun brukes fra en annen service.
     * Dersom kommandoen er trigget av en saksbehandler, forventer vi at saksbehandler har tilgang til person.
     *
     * @param forrigeUtbetaling er null dersom det ikke finnes en tidligere utbetaling
     */
    suspend fun simulerMeldekort(
        behandling: MeldekortBehandling,
        forrigeUtbetaling: Utbetaling?,
        meldeperiodeKjeder: MeldeperiodeKjeder,
        brukersNavkontor: (suspend () -> Navkontor)?,
    ): Either<KunneIkkeSimulere, SimuleringMedMetadata> {
        return utbetalingsklient.simuler(
            sakId = behandling.sakId,
            saksnummer = behandling.saksnummer,
            behandlingId = behandling.id,
            fnr = behandling.fnr,
            saksbehandler = behandling.saksbehandler!!,
            beregning = behandling.beregning!!,
            brukersNavkontor = if (brukersNavkontor != null) brukersNavkontor() else navkontorService.hentOppfolgingsenhet(behandling.fnr),
            forrigeUtbetalingJson = forrigeUtbetaling?.let {
                utbetalingRepo.hentUtbetalingJson(it.id)
            },
            forrigeUtbetalingId = forrigeUtbetaling?.id,
            meldeperiodeKjeder = meldeperiodeKjeder,
        )
    }

    /**
     * Skal kun brukes fra en annen service.
     * Dersom kommandoen er trigget av en saksbehandler, forventer vi at saksbehandler har tilgang til person.
     *
     * @param forrigeUtbetaling er null dersom det ikke finnes en tidligere utbetaling
     */
    suspend fun simulerRevurdering(
        behandling: Revurdering,
        beregning: BehandlingBeregning,
        forrigeUtbetaling: Utbetaling?,
        meldeperiodeKjeder: MeldeperiodeKjeder,
        brukersNavkontor: (suspend () -> Navkontor)?,
    ): Either<KunneIkkeSimulere, SimuleringMedMetadata> {
        return utbetalingsklient.simuler(
            sakId = behandling.sakId,
            saksnummer = behandling.saksnummer,
            behandlingId = behandling.id,
            fnr = behandling.fnr,
            saksbehandler = behandling.saksbehandler!!,
            beregning = beregning,
            brukersNavkontor = if (brukersNavkontor != null) brukersNavkontor() else navkontorService.hentOppfolgingsenhet(behandling.fnr),
            forrigeUtbetalingJson = forrigeUtbetaling?.let {
                utbetalingRepo.hentUtbetalingJson(it.id)
            },
            forrigeUtbetalingId = forrigeUtbetaling?.id,
            meldeperiodeKjeder = meldeperiodeKjeder,
        )
    }
}
