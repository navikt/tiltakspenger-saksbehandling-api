package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
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
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
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

    // utbetaldata sender beskrivelsen, ikke kodeverdien
    private val relevanteYtelsestyper = Ytelsetype.entries.toTypedArray().map { it.tekstverdi }

    private val utbetalingsinformasjonUri = URI.create("$baseUrl/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern")

    override suspend fun hentYtelserFraUtbetaldata(
        fnr: Fnr,
        periode: Periode,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, List<Ytelse>> {
        // Siden domenet lagrer perioden vi har søkt på, må den ha kontroll på dette selv, den kan ikke bli hemmelig mutert av klienten.
        if (periode.tilOgMed > LocalDate.now(clock)) throw IllegalStateException("Utbetaldata godtar ikke datoer frem i tid.")
        val jsonPayload = serialize(
            HentUtbetalingsinformasjonRequest(
                ident = fnr.verdi,
                periode = UtbetalingDto.UtbetalingsperiodeDto(
                    fom = periode.fraOgMed,
                    tom = periode.tilOgMed,
                ),
            ),
        )
        // Utbetaldata svarer alltid 200 ved suksess; alt annet skal være feil (paritet med gammel successStatus).
        return httpKlient.postJson<List<UtbetalingDto>>(
            uri = utbetalingsinformasjonUri,
            body = SerialisertJson(jsonPayload),
            headere = listOf(NavHeadere.navCallId(correlationId.toString())),
            godta = Statusregel.Eksakt(200),
        ).map { it.body.tilYtelser() }
    }

    private fun List<UtbetalingDto>.tilYtelser(): List<Ytelse> {
        val utbetalinger = this.flatMap { it.ytelseListe }
        val relevanteYtelser = utbetalinger.filter { it.ytelsestype == null || it.ytelsestype in relevanteYtelsestyper }
            .groupBy { it.ytelsestype }
        return relevanteYtelser.map { ytelse ->
            Ytelse(
                ytelsetype = Ytelsetype.entries.find { ytelse.key == it.tekstverdi } ?: Ytelsetype.UKJENT,
                perioder = ytelse.value.map {
                    Periode(
                        fraOgMed = it.ytelsesperiode.fom,
                        tilOgMed = it.ytelsesperiode.tom,
                    )
                },
            )
        }
    }
}
