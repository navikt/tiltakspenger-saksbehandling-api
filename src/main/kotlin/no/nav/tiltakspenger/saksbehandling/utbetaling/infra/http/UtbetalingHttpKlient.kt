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
import no.nav.tiltakspenger.libs.httpklient.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.get
import no.nav.tiltakspenger.libs.httpklient.post
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
 * [httpKlient] bygges som default ut fra parametrene over ([clock], [authTokenProvider], [connectTimeout], [statusTimeout]) slik at hele klientoppsettet kan leses ett sted.
 * Sender man inn en egen [httpKlient] (typisk `HttpKlientFake` i test), **ignoreres** de parametrene som kun brukes til å bygge default-klienten.
 *
 * @param clock Klokke som sendes videre til [HttpKlient] og brukes til simuleringstidspunkt-fallback.
 * @param authTokenProvider Henter system-token mot helved. Ignoreres hvis [httpKlient] sendes inn.
 * @param connectTimeout Connect-timeout for default-klienten. Ignoreres hvis [httpKlient] sendes inn.
 * @param statusTimeout Per-request timeout for statusoppslag; også default-timeout for default-klienten.
 * @param iverksettTimeout Per-request timeout for iverksetting.
 * @param simuleringTimeout Per-request timeout for simulering. Simulering mot Oppdrag kan være treg, derav den høye defaulten.
 */
class UtbetalingHttpKlient(
    private val baseUrl: String,
    private val clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 5.seconds,
    private val statusTimeout: Duration = 15.seconds,
    private val iverksettTimeout: Duration = 30.seconds,
    private val simuleringTimeout: Duration = 45.seconds,
    private val httpKlient: HttpKlient = HttpKlient(clock = clock) {
        this.connectTimeout = connectTimeout
        this.defaultTimeout = statusTimeout
        this.successStatus = { it == 200 }
        this.authTokenProvider = authTokenProvider
    },
) : Utbetalingsklient {
    private val iverksettUri = URI.create("$baseUrl/api/iverksetting/v2")
    private val simuleringUri = URI.create("$baseUrl/api/simulering/v2")

    override suspend fun iverksett(
        utbetaling: VedtattUtbetaling,
        forrigeUtbetalingJson: String?,
        correlationId: CorrelationId,
    ): Either<KunneIkkeUtbetale, SendtUtbetaling> {
        val jsonPayload = utbetaling.toUtbetalingRequestDTO(forrigeUtbetalingJson)
        return httpKlient.post<String>(iverksettUri, jsonPayload) {
            timeout = iverksettTimeout
            // Dette er kun for vår del, open telemetry vil kunne være et alternativ. Slack tråd: https://nav-it.slack.com/archives/C06SJTR2X3L/p1724072054018589
            header("Nav-Call-Id", correlationId.value)
            successStatus(202, 409)
        }.mapLeft { feil ->
            KunneIkkeUtbetale(request = jsonPayload, feil = feil)
        }.flatMap { response ->
            when {
                response.statusCode == 202 -> SendtUtbetaling(
                    request = jsonPayload,
                    response = response.body,
                    responseStatus = response.statusCode,
                    alleredeMottattTidligere = false,
                ).right()

                // TODO post-mvp jah: På sikt er dette en litt skjør sjekk som kan føre til at vi må endre denne sjekken dersom helved forandrer meldingen. Vi har bestilt et ønske fra helved om at vi får en json-respons med en kontraktsfestet kode, evt. at de garanterer at 409 kun brukes til dedupformål.
                response.body.contains("Iverksettingen er allerede mottatt") -> SendtUtbetaling(
                    request = jsonPayload,
                    response = response.body,
                    responseStatus = response.statusCode,
                    alleredeMottattTidligere = true,
                ).right()

                // 409 uten dedup-meldingen: serveren svarte, men vi tør ikke anta at utbetalingen er mottatt.
                else -> KunneIkkeUtbetale(
                    request = jsonPayload,
                    feil = HttpKlientError.UventetStatus(
                        statusCode = response.statusCode,
                        body = response.body,
                        metadata = response.metadata,
                    ),
                ).left()
            }
        }
    }

    override suspend fun hentUtbetalingsstatus(
        utbetaling: UtbetalingDetSkalHentesStatusFor,
    ): Either<HttpKlientError, Utbetalingsstatus> {
        val uri = URI.create("$baseUrl/api/iverksetting/${utbetaling.saksnummer.verdi}/${utbetaling.utbetalingId.uuidPart()}/status")
        return httpKlient.get<IverksettStatus>(uri) {
            timeout = statusTimeout
        }.map { it.body.tilUtbetalingsstatus() }
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
        return httpKlient.post<String>(simuleringUri, jsonPayload) {
            timeout = simuleringTimeout
            // 204 betyr at simuleringen ikke gir noen endring i utbetalingen.
            successStatus(200, 204)
        }.mapLeft { it.tilKunneIkkeSimulere() }
            .flatMap { response ->
                if (response.statusCode == 204) {
                    SimuleringMedMetadata(
                        simulering = Simulering.IngenEndring(
                            simuleringstidspunkt = response.metadata.tidsstempler.responsMottatt ?: nå(clock),
                        ),
                        originalResponseBody = response.body,
                    ).right()
                } else {
                    Either.catch {
                        SimuleringMedMetadata(
                            simulering = response.body.toSimuleringFraHelvedResponse(
                                meldeperiodeKjeder = meldeperiodeKjeder,
                                clock = clock,
                            ),
                            originalResponseBody = response.body,
                        )
                    }.mapLeft { throwable ->
                        KunneIkkeSimulere.UkjentFeil(
                            HttpKlientError.DeserializationError(
                                throwable = throwable,
                                body = response.body,
                                statusCode = response.statusCode,
                                metadata = response.metadata,
                            ),
                        )
                    }
                }
            }
    }

    private fun IverksettStatus.tilUtbetalingsstatus(): Utbetalingsstatus = when (this) {
        IverksettStatus.SENDT_TIL_OPPDRAG -> Utbetalingsstatus.SendtTilOppdrag
        IverksettStatus.FEILET_MOT_OPPDRAG -> Utbetalingsstatus.FeiletMotOppdrag
        IverksettStatus.OK -> Utbetalingsstatus.Ok
        IverksettStatus.IKKE_PÅBEGYNT -> Utbetalingsstatus.IkkePåbegynt
        IverksettStatus.OK_UTEN_UTBETALING -> Utbetalingsstatus.OkUtenUtbetaling
    }

    private fun HttpKlientError.tilKunneIkkeSimulere(): KunneIkkeSimulere = when {
        this is HttpKlientError.Timeout -> KunneIkkeSimulere.Timeout(this)

        // OS har åpningstider; 503 betyr stengt/vedlikehold, ikke en feil hos oss.
        this is HttpKlientError.UventetStatus && statusCode == 503 -> KunneIkkeSimulere.Stengt(this)

        else -> KunneIkkeSimulere.UkjentFeil(this)
    }
}
