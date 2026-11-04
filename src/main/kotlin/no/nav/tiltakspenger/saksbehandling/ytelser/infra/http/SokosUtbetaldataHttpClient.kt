package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

import arrow.core.Either
import arrow.core.flatMap
import io.micrometer.core.instrument.MeterRegistry
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
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
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.tilUtbetalingsoversiktMetadata
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.SokosUtbetaldataClient
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.YtelserMedMetadata
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.request.SokosHentUtbetalingsinformasjonRequest
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.response.SokosUtbetalingResponse
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.response.tellUkjenteYtelsestyper
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.response.tilYtelsetype
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.http.response.utelatteYtelsestyper
import java.net.URI
import java.time.Clock
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Klient mot sokos-utbetaldata for utbetalingshistorikk fra økonomisystemene.
 *
 * Kildekode: https://github.com/navikt/sokos-utbetaldata
 * Dokumentasjon: -
 * API-spec: https://sokos-utbetaldata.dev.intern.nav.no/utbetaldata/api/v2/docs (Swagger) og https://github.com/navikt/sokos-utbetaldata/blob/main/src/main/resources/spec/utbetaldata-v2-openapi-spec.yaml (Spec)
 * Slack: #utbetaling
 * Teamkatalog: https://teamkatalogen.nav.no/team/6260623b-d58e-4c17-8861-0a7b92fdc1e2
 */
class SokosUtbetaldataHttpClient(
    baseUrl: String,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 3.seconds,
    timeout: Duration = 6.seconds,
    private val clock: Clock,
    private val meterRegistry: MeterRegistry,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : SokosUtbetaldataClient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
        ),
        transport = transport,
    )

    private val utbetalingsinformasjonUri = URI.create("$baseUrl/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern")

    override suspend fun hentYtelserFraUtbetaldata(
        fnr: Fnr,
        periode: Periode,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, YtelserMedMetadata> {
        // Siden domenet lagrer perioden vi har søkt på, må den ha kontroll på dette selv, den kan ikke bli hemmelig mutert av klienten.
        if (periode.tilOgMed > LocalDate.now(clock)) throw IllegalStateException("Utbetaldata godtar ikke datoer frem i tid.")
        val jsonPayload = serialize(
            SokosHentUtbetalingsinformasjonRequest(
                ident = fnr.verdi,
                periode = SokosHentUtbetalingsinformasjonRequest.UtbetalingsperiodeDto(
                    fom = periode.fraOgMed,
                    tom = periode.tilOgMed,
                ),
            ),
        )
        // Utbetaldata svarer alltid 200 ved suksess; alt annet skal være feil (paritet med gammel successStatus).
        return httpKlient.postJson<List<SokosUtbetalingResponse>>(
            uri = utbetalingsinformasjonUri,
            body = SerialisertJson(jsonPayload),
            headere = listOf(NavHeadere.navCallId(correlationId.toString())),
            godta = Statusregel.Eksakt(200),
        )
            // Mappingen validerer perioder fra svaret (Periode-init); tryMap gjør et ugyldig svar til typet feil med responsens metadata i stedet for en ukontrollert exception.
            .flatMap { response ->
                response.tryMap {
                    YtelserMedMetadata(
                        ytelser = it.tilYtelser(),
                        // DTO-en leser bare ytelsestype og ytelsesperiode, så kontonummer og navn fra svaret blir ikke med.
                        metadata = response.metadata.tilUtbetalingsoversiktMetadata(correlationId).copy(svar = serialize(it)),
                    )
                }
            }
    }

    /** Utelatte typer forkastes; hver tekst blir én ytelse, så begge stavemåtene av samme type gir to. */
    private fun List<SokosUtbetalingResponse>.tilYtelser(): List<Ytelse> {
        val utbetalinger = this.flatMap { it.ytelseListe }
        meterRegistry.tellUkjenteYtelsestyper(utbetalinger.map { it.ytelsestype })
        return utbetalinger
            .filterNot { it.ytelsestype in utelatteYtelsestyper }
            .groupBy { it.ytelsestype }
            .map { (kildetekst, ytelser) ->
                val perioder = ytelser.map { Periode(fraOgMed = it.ytelsesperiode.fom, tilOgMed = it.ytelsesperiode.tom) }
                val ytelsetype = kildetekst?.tilYtelsetype()
                if (ytelsetype == null) {
                    Ytelse.Ukjent(kildetekst = kildetekst, perioder = perioder)
                } else {
                    Ytelse.Kjent(ytelsetype = ytelsetype, kildetekst = kildetekst, perioder = perioder)
                }
            }
    }
}
