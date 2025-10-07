package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
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
     * @param behandling Forventer at behandling.beregning og behandling.saksbehandler er oppdatert
     */
    suspend fun simulerMeldekort(
        behandling: MeldekortBehandling,
        forrigeUtbetaling: VedtattUtbetaling?,
        meldeperiodeKjeder: MeldeperiodeKjeder,
        brukersNavkontor: (suspend () -> Navkontor)?,
    ): Either<KunneIkkeSimulere, SimuleringMedMetadata> {
        return simuler(
            sakId = behandling.sakId,
            saksnummer = behandling.saksnummer,
            behandlingId = behandling.id,
            fnr = behandling.fnr,
            saksbehandler = behandling.saksbehandler!!,
            beregning = behandling.beregning!!,
            forrigeUtbetaling = forrigeUtbetaling,
            meldeperiodeKjeder = meldeperiodeKjeder,
            brukersNavkontor = brukersNavkontor,
        )
    }

    /**
     * Skal kun brukes fra en annen service.
     * Dersom kommandoen er trigget av en saksbehandler, forventer vi at saksbehandler har tilgang til person.
     *
     * @param forrigeUtbetaling er null dersom det ikke finnes en tidligere utbetaling
     */
    suspend fun simulerSøknadsbehandlingEllerRevurdering(
        behandling: Rammebehandling,
        beregning: Beregning,
        forrigeUtbetaling: VedtattUtbetaling?,
        meldeperiodeKjeder: MeldeperiodeKjeder,
        saksbehandler: String,
        brukersNavkontor: (suspend () -> Navkontor)?,
    ): Either<KunneIkkeSimulere, SimuleringMedMetadata> {
        return simuler(
            sakId = behandling.sakId,
            saksnummer = behandling.saksnummer,
            behandlingId = behandling.id,
            fnr = behandling.fnr,
            saksbehandler = saksbehandler,
            beregning = beregning,
            forrigeUtbetaling = forrigeUtbetaling,
            meldeperiodeKjeder = meldeperiodeKjeder,
            brukersNavkontor = brukersNavkontor,
        )
    }

    suspend fun simuler(
        sakId: SakId,
        saksnummer: Saksnummer,
        behandlingId: Ulid,
        fnr: Fnr,
        beregning: Beregning,
        forrigeUtbetaling: VedtattUtbetaling?,
        meldeperiodeKjeder: MeldeperiodeKjeder,
        saksbehandler: String,
        brukersNavkontor: (suspend () -> Navkontor)?,
    ): Either<KunneIkkeSimulere, SimuleringMedMetadata> {
        return utbetalingsklient.simuler(
            sakId = sakId,
            saksnummer = saksnummer,
            behandlingId = behandlingId,
            fnr = fnr,
            saksbehandler = saksbehandler,
            beregning = beregning,
            brukersNavkontor = if (brukersNavkontor != null) brukersNavkontor() else navkontorService.hentOppfolgingsenhet(fnr),
            forrigeUtbetalingJson = forrigeUtbetaling?.let {
                utbetalingRepo.hentUtbetalingJson(it.id)
            },
            forrigeUtbetalingId = forrigeUtbetaling?.id,
            meldeperiodeKjeder = meldeperiodeKjeder,
        )
    }
}
