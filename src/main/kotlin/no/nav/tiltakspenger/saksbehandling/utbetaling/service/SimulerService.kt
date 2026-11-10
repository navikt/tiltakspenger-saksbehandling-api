package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.V3Simuleringsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.logg

class SimulerService(
    private val utbetalingsklient: Utbetalingsklient,
    private val v3Simuleringsklient: V3Simuleringsklient,
    private val navkontorService: NavkontorService,
    private val utbetalingRepo: UtbetalingRepo,
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Skal kun brukes fra en annen service.
     * Dersom kommandoen er trigget av en saksbehandler, forventer vi at saksbehandler har tilgang til person.
     *
     * @param forrigeUtbetaling er null dersom det ikke finnes en tidligere utbetaling
     * @param behandling Forventer at behandling.beregning og behandling.saksbehandler er oppdatert
     * @param beregning defaulter til beregningen på behandlingen.
     * Ved kontrollsimulering sendes en fersk beregning inn, siden den skal simuleres uten å endre behandlingen.
     */
    suspend fun simulerMeldekort(
        behandling: Meldekortbehandling,
        forrigeUtbetaling: VedtattUtbetaling?,
        meldeperiodeKjeder: MeldeperiodeKjeder,
        kanSendeInnHelgForMeldekort: Boolean,
        beregning: Beregning = behandling.beregning!!,
        brukersNavkontor: (suspend () -> Navkontor)?,
    ): Either<KunneIkkeSimulere, SimuleringMedMetadata> {
        return simuler(
            sakId = behandling.sakId,
            saksnummer = behandling.saksnummer,
            behandlingId = behandling.id,
            fnr = behandling.fnr,
            saksbehandler = behandling.saksbehandler!!,
            beregning = beregning,
            forrigeUtbetaling = forrigeUtbetaling,
            meldeperiodeKjeder = meldeperiodeKjeder,
            brukersNavkontor = brukersNavkontor,
            kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
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
        kanSendeInnHelgForMeldekort: Boolean,
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
            kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
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
        kanSendeInnHelgForMeldekort: Boolean,
        brukersNavkontor: (suspend () -> Navkontor)?,
    ): Either<KunneIkkeSimulere, SimuleringMedMetadata> {
        val oppdragsversjon = utbetalingRepo.hentOppdragsversjon(sakId)
        val forrigeUtbetalingJson = forrigeUtbetaling?.let {
            utbetalingRepo.hentUtbetalingJson(it.id)
        }
        val navkontor = if (brukersNavkontor != null) {
            brukersNavkontor()
        } else {
            navkontorService.hentNavkontor(
                fnr = fnr,
                loggkontekst = "sakId: $sakId, saksnummer: ${saksnummer.verdi}, behandlingId: $behandlingId",
            )
        }
        val simulering = when (oppdragsversjon) {
            Oppdragsversjon.V2 -> utbetalingsklient.simuler(
                sakId = sakId,
                saksnummer = saksnummer,
                behandlingId = behandlingId,
                fnr = fnr,
                saksbehandler = saksbehandler,
                beregning = beregning,
                brukersNavkontor = navkontor,
                forrigeUtbetalingJson = forrigeUtbetalingJson,
                forrigeUtbetalingId = forrigeUtbetaling?.id,
                meldeperiodeKjeder = meldeperiodeKjeder,
                kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
            )

            Oppdragsversjon.V3 -> v3Simuleringsklient.simuler(
                sakId = sakId,
                saksnummer = saksnummer,
                behandlingId = behandlingId,
                fnr = fnr,
                beregning = beregning,
                forrigeUtbetalingJson = forrigeUtbetalingJson,
                meldeperiodeKjeder = meldeperiodeKjeder,
                saksbehandler = saksbehandler,
                brukersNavkontor = navkontor,
            )
        }
        return simulering.onLeft {
            it.logg(logger, kontekst = "sakId: $sakId, saksnummer: ${saksnummer.verdi}, behandlingId: $behandlingId")
        }
    }
}
