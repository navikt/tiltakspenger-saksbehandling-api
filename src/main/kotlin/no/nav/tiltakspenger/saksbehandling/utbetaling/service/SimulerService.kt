package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.Utbetalingsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.UtbetalingsvedtakRepo

class SimulerService(
    private val utbetalingsklient: Utbetalingsklient,
    private val navkontorService: NavkontorService,
    private val utbetalingsvedtakRepo: UtbetalingsvedtakRepo,
) {
    /**
     * Skal kun brukes fra en annen service.
     * Dersom kommandoen er trigget av en saksbehandler, forventer vi at saksbehandler har tilgang til person.
     * Lag egen funksjon for revurderinger (typisk opphør av utbetalte perioder / endring av barnetillegg)
     *
     * @param forrigeUtbetaling er null dersom det ikke finnes en tidligere utbetaling
     */
    suspend fun simulerMeldekort(
        behandling: MeldekortBehandling,
        forrigeUtbetaling: Utbetalingsvedtak?,
        meldeperiodeKjeder: MeldeperiodeKjeder,
        brukersNavkontor: (suspend () -> Navkontor)?,
    ): Either<KunneIkkeSimulere, SimuleringMedMetadata> {
        return utbetalingsklient.simuler(
            behandling = behandling,
            brukersNavkontor = if (brukersNavkontor != null) brukersNavkontor() else navkontorService.hentOppfolgingsenhet(behandling.fnr),
            forrigeUtbetalingJson = forrigeUtbetaling?.let {
                utbetalingsvedtakRepo.hentUtbetalingJsonForVedtakId(it.id)
            },
            forrigeVedtakId = forrigeUtbetaling?.id,
            meldeperiodeKjeder = meldeperiodeKjeder,
        )
    }
}
