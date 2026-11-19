@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.objectmothers.DevSimuleringsscenario
import no.nav.tiltakspenger.saksbehandling.objectmothers.genererSimuleringFraBeregning
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.TilbakekrevingFakeProducer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.V3Simuleringsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

class UtbetalingFakeKlient(
    private val sakRepo: SakRepo,
    private val clock: Clock = fixedClock,
    private val tilbakekrevingProducer: TilbakekrevingFakeProducer,
    var skalStarteTilbakekrevinger: Boolean,
    /**
     * Settes av tester som trenger en utbetaling som ikke gikk gjennom, f.eks. [Utbetalingsstatus.FeiletMotOppdrag].
     *
     * **Testen må da kjøre isolert** (`runIsolated = true` + `@IsolatedDatabaseTest`).
     * Denne faken er per test-kontekst, mens `oppdaterUtbetalingsstatus`-jobben går gjennom alle utbetalinger i skjemaet.
     * Deler du skjema med andre route-tester, vil deres jobbkjøring hente statusen for din utbetaling gjennom deres fake og overskrive den med `Ok`.
     */
    var utbetalingsstatus: Utbetalingsstatus = Utbetalingsstatus.Ok,
    /**
     * Settes av tester som trenger at selve iverksettingen mot helved feiler, f.eks. for å dekke lagringen av feilresponsen.
     *
     * **Testen må da kjøre isolert** (`runIsolated = true` + `@IsolatedDatabaseTest`), av samme grunn som [utbetalingsstatus].
     * `sendUtbetalingerTilHelved`-jobben går gjennom alle utbetalinger i skjemaet, så en annen test ville fått sin utbetaling feilet av din fake.
     */
    var iverksettFeil: KunneIkkeUtbetale? = null,
    /**
     * Settes av tester som trenger at simuleringen feiler, f.eks. for at en behandling skal lagres med beregning uten simulering.
     * Simuleringen kalles synkront fra behandlingens egne ruter og ikke fra en jobb som går gjennom hele skjemaet, så testen trenger ikke kjøre isolert.
     */
    var simulerFeil: KunneIkkeSimulere? = null,
    /**
     * Settes av tester som trenger utbetalinger vi har sendt, men som oppdragssystemet ikke har utbetalt ennå.
     * Simuleringen regnes da ut fra saken slik den var da oppdragssystemet sist utbetalte, slik at de senere utbetalingene verken er tidligere utbetalt eller gir feilutbetaling.
     * Simuleringen kalles synkront, som for [simulerFeil], så testen trenger ikke kjøre isolert.
     */
    var sakUtbetaltAvOppdrag: Sak? = null,
    /**
     * Settes av tester som trenger et bestemt svar fra simuleringen, f.eks. et som vår egen beregning ikke kan gi opphav til.
     * Simuleringen kalles synkront, som for [simulerFeil], så testen trenger ikke kjøre isolert.
     */
    var simulering: SimuleringMedMetadata? = null,
) : Utbetalingsklient,
    V3Simuleringsklient {

    /**
     * Utbetalinger som skal starte tilbakekreving når oppdragssystemet har utbetalt dem.
     * [iverksett] kalles inne i transaksjonen som markerer utbetalingen som sendt, og tilbakekrevingen åpner egne databasesesjoner.
     * Den startes derfor først når statusen hentes, som skjer utenfor transaksjonen og etter at den er committet.
     */
    private val utbetalingerSomVenterPåTilbakekreving = ConcurrentHashMap<UtbetalingId, VedtattUtbetaling>()

    override suspend fun iverksett(
        utbetaling: VedtattUtbetaling,
        forrigeUtbetalingJson: String?,
        correlationId: CorrelationId,
    ): Either<KunneIkkeUtbetale, SendtUtbetaling> {
        iverksettFeil?.let { return it.left() }

        val response = SendtUtbetaling(
            utbetaling.toUtbetalingRequestDTO(forrigeUtbetalingJson),
            "response - ${utbetaling.id}",
            responseStatus = 202,
            alleredeMottattTidligere = false,
        )

        if (skalStarteTilbakekrevinger) {
            utbetalingerSomVenterPåTilbakekreving[utbetaling.id] = utbetaling
        }

        return response.right()
    }

    override suspend fun hentUtbetalingsstatus(
        utbetaling: UtbetalingDetSkalHentesStatusFor,
    ): Either<HttpKlientError, Utbetalingsstatus> {
        if (utbetalingsstatus.erOK()) {
            utbetalingerSomVenterPåTilbakekreving.remove(utbetaling.utbetalingId)
                ?.let { tilbakekrevingProducer.produserInfoBehovVedFeilutbetaling(it) }
        }
        return utbetalingsstatus.right()
    }

    override suspend fun simuler(
        sakId: SakId,
        saksnummer: Saksnummer,
        behandlingId: Ulid,
        fnr: Fnr,
        saksbehandler: String,
        beregning: Beregning,
        brukersNavkontor: Navkontor,
        kanSendeInnHelgForMeldekort: Boolean,
        forrigeUtbetalingJson: String?,
        forrigeUtbetalingId: UtbetalingId?,
        meldeperiodeKjeder: MeldeperiodeKjeder,
    ): Either<KunneIkkeSimulere, SimuleringMedMetadata> {
        simulerFeil?.let { return it.left() }
        simulering?.let { return it.right() }

        val sak = sakUtbetaltAvOppdrag ?: sakRepo.hentForSakId(sakId)!!
        return sak.genererSimuleringFraBeregning(
            beregning = beregning,
            clock = clock,
            scenario = DevSimuleringsscenario.fraFnr(fnr),
        ).right()
    }

    override suspend fun simuler(
        sakId: SakId,
        saksnummer: Saksnummer,
        behandlingId: Ulid,
        fnr: Fnr,
        beregning: Beregning,
        forrigeUtbetalingJson: String?,
        meldeperiodeKjeder: MeldeperiodeKjeder,
        saksbehandler: String,
        brukersNavkontor: Navkontor,
    ): Either<KunneIkkeSimulere, SimuleringMedMetadata> = simuler(
        sakId = sakId,
        saksnummer = saksnummer,
        behandlingId = behandlingId,
        fnr = fnr,
        beregning = beregning,
        forrigeUtbetalingJson = forrigeUtbetalingJson,
        forrigeUtbetalingId = null,
        meldeperiodeKjeder = meldeperiodeKjeder,
        saksbehandler = saksbehandler,
        kanSendeInnHelgForMeldekort = false,
        brukersNavkontor = brukersNavkontor,
    )
}
