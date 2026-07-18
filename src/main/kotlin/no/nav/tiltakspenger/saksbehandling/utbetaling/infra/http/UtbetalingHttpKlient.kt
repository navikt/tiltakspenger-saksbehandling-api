package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.harStatus
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.NavHeadere
import no.nav.tiltakspenger.libs.httpklient.infra.kall.SerialisertJson
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.libs.httpklient.tryMap
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.iverksett.IverksettStatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.SendtUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.ports.Utbetalingsklient
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Klient mot helved utsjekk (iverksetting, utbetalingsstatus og simulering), bygget på den felles [HttpKlient]-modulen i tiltakspenger-libs.
 *
 * Kildekode: https://github.com/navikt/helved-utbetaling
 * Dokumentasjon: https://navikt.github.io/utsjekk-docs/ og https://helved-docs.intern.dev.nav.no/
 * API-spec: iverksetting https://helved-docs.intern.dev.nav.no/v2/doc/, status https://helved-docs.intern.dev.nav.no/v2/doc/status, simulering https://helved-docs.intern.dev.nav.no/v2/doc/simulering
 * Slack: #team-hel-ved
 * Teamkatalog: https://teamkatalogen.nav.no/team/93e78ae9-babc-452c-965d-e7dc67af8612
 *
 * Klienten logger bevisst ikke selv: den bærer HTTP-konteksten videre via [HttpKlientError] (evt. wrappet i [KunneIkkeUtbetale]/[KunneIkkeSimulere]), og feillogging gjøres én gang i kallende service ([no.nav.tiltakspenger.saksbehandling.utbetaling.service.SendUtbetalingerService], [no.nav.tiltakspenger.saksbehandling.utbetaling.service.OppdaterUtbetalingsstatusService], [no.nav.tiltakspenger.saksbehandling.utbetaling.service.SimulerService]), som i tillegg har domenekonteksten.
 *
 * Per-kall-timeouts finnes ikke i v2-API-et; de tre endepunktene har ulike behov og får derfor hver sin [HttpKlient]-instans som deler samme [transport].
 *
 * @param clock Klokke som sendes videre til [HttpKlient] og brukes til simuleringstidspunkt-fallback.
 * @param authTokenProvider Henter system-token mot helved.
 * @param statusTimeout Timeout for statusoppslag.
 * @param iverksettTimeout Timeout for iverksetting.
 * @param simuleringTimeout Timeout for simulering.
 * Simulering mot Oppdrag kan være treg, derav den høye defaulten.
 * @param transport Nettverks-sømmen; default er produksjonstransporten, tester sender inn `FakeHttpTransport` slik at hele den reelle pipelinen kjører.
 */
class UtbetalingHttpKlient(
    private val baseUrl: String,
    private val clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 5.seconds,
    statusTimeout: Duration = 15.seconds,
    iverksettTimeout: Duration = 30.seconds,
    simuleringTimeout: Duration = 45.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : Utbetalingsklient {
    private val statusKlient = utbetalingsklient(clock, authTokenProvider, connectTimeout, statusTimeout, transport)
    private val iverksettKlient = utbetalingsklient(clock, authTokenProvider, connectTimeout, iverksettTimeout, transport)
    private val simuleringsKlient = utbetalingsklient(clock, authTokenProvider, connectTimeout, simuleringTimeout, transport)

    private val iverksettUri = URI.create("$baseUrl/api/iverksetting/v2")
    private val simuleringUri = URI.create("$baseUrl/api/simulering/v2")

    override suspend fun iverksett(
        utbetaling: VedtattUtbetaling,
        forrigeUtbetalingJson: String?,
        correlationId: CorrelationId,
    ): Either<KunneIkkeUtbetale, SendtUtbetaling> {
        val jsonPayload = utbetaling.toUtbetalingRequestDTO(forrigeUtbetalingJson)
        return iverksettKlient.postJsonUtenSvar(
            uri = iverksettUri,
            body = SerialisertJson(jsonPayload),
            // Dette er kun for vår del, open telemetry vil kunne være et alternativ.
            // Slack tråd: https://nav-it.slack.com/archives/C06SJTR2X3L/p1724072054018589
            headere = listOf(NavHeadere.navCallId(correlationId.value)),
            godta = Statusregel.Eksakt(202),
        ).fold(
            ifRight = { response ->
                SendtUtbetaling(
                    request = jsonPayload,
                    response = response.rawResponseString,
                    responseStatus = response.statusCode,
                    alleredeMottattTidligere = false,
                ).right()
            },
            ifLeft = { feil ->
                when {
                    // 409 er et domeneutfall (dedup) som utledes fra feiltypen, ikke en suksess-status.
                    // TODO post-mvp jah: På sikt er dette en litt skjør sjekk som kan føre til at vi må endre denne sjekken dersom helved forandrer meldingen.
                    // Vi har bestilt et ønske fra helved om at vi får en json-respons med en kontraktsfestet kode, evt. at de garanterer at 409 kun brukes til dedupformål.
                    feil.harStatus(409) && feil is HttpKlientError.UventetStatus && feil.body.contains("Iverksettingen er allerede mottatt") -> SendtUtbetaling(
                        request = jsonPayload,
                        response = feil.body,
                        responseStatus = feil.statusCode,
                        alleredeMottattTidligere = true,
                    ).right()

                    // Alt annet (inkl. 409 uten dedup-meldingen, der vi ikke tør anta at utbetalingen er mottatt): feilen videre urørt.
                    else -> KunneIkkeUtbetale(request = jsonPayload, feil = feil).left()
                }
            },
        )
    }

    override suspend fun hentUtbetalingsstatus(
        utbetaling: UtbetalingDetSkalHentesStatusFor,
    ): Either<HttpKlientError, Utbetalingsstatus> {
        val uri = URI.create("$baseUrl/api/iverksetting/${utbetaling.saksnummer.verdi}/${utbetaling.utbetalingId.uuidPart()}/status")
        // Helved svarer alltid 200 ved suksess; alt annet skal være feil (paritet med gammel successStatus).
        return statusKlient.getJson<IverksettStatus>(uri, godta = Statusregel.Eksakt(200)).map { it.body.tilUtbetalingsstatus() }
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
        val jsonPayload = toSimuleringRequest(
            saksnummer = saksnummer,
            behandlingId = behandlingId,
            fnr = fnr,
            saksbehandler = saksbehandler,
            beregning = beregning,
            brukersNavkontor = brukersNavkontor,
            forrigeUtbetalingJson = forrigeUtbetalingJson,
            forrigeUtbetalingId = forrigeUtbetalingId,
        )
        // 204 betyr at simuleringen ikke gir noen endring i utbetalingen; helved svarer ellers 200.
        return simuleringsKlient.postJsonEllerNull<SimuleringResponseDTO>(
            uri = simuleringUri,
            body = SerialisertJson(jsonPayload),
            nullVedStatus = setOf(204),
            godta = Statusregel.Eksakt(200),
        ).mapLeft { it.tilKunneIkkeSimulere() }
            .flatMap { response ->
                val dto = response.body
                if (dto == null) {
                    SimuleringMedMetadata(
                        simulering = Simulering.IngenEndring(
                            simuleringstidspunkt = simuleringstidspunkt(response.metadata.tidsstempler.responsMottatt, clock),
                        ),
                        originalResponseBody = response.rawResponseString,
                    ).right()
                } else {
                    // Domene-mapping som kan feile (fnr-/saksnummer-sjekkene) blir en typet feil med responsens metadata.
                    response.tryMap {
                        SimuleringMedMetadata(
                            simulering = dto.toSimuleringFraHelvedResponse(
                                meldeperiodeKjeder = meldeperiodeKjeder,
                                clock = clock,
                            ),
                            originalResponseBody = response.rawResponseString,
                        )
                    }.mapLeft { KunneIkkeSimulere.UkjentFeil(it) }
                }
            }
    }

    companion object {
        /** Pipelinen setter responsMottatt på alle ekte responser; fallbacken dekker håndbygde/ufullstendige metadata. */
        internal fun simuleringstidspunkt(responsMottatt: java.time.LocalDateTime?, clock: Clock): java.time.LocalDateTime =
            responsMottatt ?: nå(clock)
    }

    /** Én instans per endepunkt fordi timeoutene er ulike; transporten (og dermed connection-pool i produksjon) deles. */
    private fun utbetalingsklient(
        clock: Clock,
        authTokenProvider: AuthTokenProvider,
        connectTimeout: Duration,
        timeout: Duration,
        transport: HttpTransport,
    ): HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
        ),
        transport = transport,
    )

    private fun IverksettStatus.tilUtbetalingsstatus(): Utbetalingsstatus = when (this) {
        IverksettStatus.SENDT_TIL_OPPDRAG -> Utbetalingsstatus.SendtTilOppdrag
        IverksettStatus.FEILET_MOT_OPPDRAG -> Utbetalingsstatus.FeiletMotOppdrag
        IverksettStatus.OK -> Utbetalingsstatus.Ok
        IverksettStatus.IKKE_PÅBEGYNT -> Utbetalingsstatus.IkkePåbegynt
        IverksettStatus.OK_UTEN_UTBETALING -> Utbetalingsstatus.OkUtenUtbetaling
    }

    private fun HttpKlientError.tilKunneIkkeSimulere(): KunneIkkeSimulere = when (this) {
        is HttpKlientError.Timeout -> KunneIkkeSimulere.Timeout(this)

        // OS har åpningstider; 503 betyr stengt/vedlikehold, ikke en feil hos oss.
        is HttpKlientError.UventetStatus if statusCode == 503 -> KunneIkkeSimulere.Stengt(this)

        else -> KunneIkkeSimulere.UkjentFeil(this)
    }
}
