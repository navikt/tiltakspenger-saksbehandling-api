package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
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
import no.nav.tiltakspenger.saksbehandling.person.Fødselsnumre
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktMetadata
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

    /** Slår opp hver ident etter tur og slår sammen ytelsene, slik at samme type fra flere identer blir én ytelse. */
    override suspend fun hentYtelserFraUtbetaldata(
        fødselsnumre: Fødselsnumre,
        periode: Periode,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, YtelserMedMetadata> {
        // Siden domenet lagrer perioden vi har søkt på, må den ha kontroll på dette selv, den kan ikke bli hemmelig mutert av klienten.
        if (periode.tilOgMed > LocalDate.now(clock)) throw IllegalStateException("Utbetaldata godtar ikke datoer frem i tid.")
        return either {
            val svar = fødselsnumre.alle.map { hentForIdent(it, periode, correlationId).bind() }
            YtelserMedMetadata(
                ytelser = svar.toList().flatMap { it.ytelser }.tilYtelser(),
                metadata = svar.map { it.metadata },
            )
        }
    }

    private suspend fun hentForIdent(
        ident: Fnr,
        periode: Periode,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, SvarForIdent> {
        val jsonPayload = serialize(
            SokosHentUtbetalingsinformasjonRequest(
                ident = ident.verdi,
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
                    SvarForIdent(
                        ytelser = it.flatMap { utbetaling -> utbetaling.ytelseListe }.map { ytelse ->
                            MottattYtelse(
                                kildetekst = ytelse.ytelsestype,
                                periode = Periode(fraOgMed = ytelse.ytelsesperiode.fom, tilOgMed = ytelse.ytelsesperiode.tom),
                            )
                        },
                        // DTO-en leser bare ytelsestype og ytelsesperiode, så kontonummer og navn fra svaret blir ikke med.
                        metadata = response.metadata.tilUtbetalingsoversiktMetadata(correlationId).copy(svar = serialize(it)),
                    )
                }
            }
    }

    private data class MottattYtelse(val kildetekst: String?, val periode: Periode)

    private data class SvarForIdent(val ytelser: List<MottattYtelse>, val metadata: UtbetalingsoversiktMetadata)

    /** Utelatte typer forkastes; hver tekst blir én ytelse, så begge stavemåtene av samme type gir to. */
    private fun List<MottattYtelse>.tilYtelser(): List<Ytelse> {
        meterRegistry.tellUkjenteYtelsestyper(map { it.kildetekst })
        return this
            .filterNot { it.kildetekst in utelatteYtelsestyper }
            .groupBy { it.kildetekst }
            .map { (kildetekst, ytelser) ->
                val perioder = ytelser.map { it.periode }
                val ytelsetype = kildetekst?.tilYtelsetype()
                if (ytelsetype == null) {
                    Ytelse.Ukjent(kildetekst = kildetekst, perioder = perioder)
                } else {
                    Ytelse.Kjent(ytelsetype = ytelsetype, kildetekst = kildetekst, perioder = perioder)
                }
            }
    }
}
