package no.nav.tiltakspenger.saksbehandling.ytelser.infra.http

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.post
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
import java.net.URI
import java.time.Clock
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// swagger: https://sokos-utbetaldata.dev.intern.nav.no/utbetaldata/api/v2/docs
class SokosUtbetaldataHttpClient(
    baseUrl: String,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 3.seconds,
    private val timeout: Duration = 6.seconds,
    private val clock: Clock,
    private val httpKlient: HttpKlient = HttpKlient(clock = clock) {
        this.connectTimeout = connectTimeout
        this.defaultTimeout = timeout
        this.successStatus = { it == 200 }
        this.authTokenProvider = authTokenProvider
    },
) : SokosUtbetaldataClient {
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
        return httpKlient.post<List<UtbetalingDto>>(utbetalingsinformasjonUri, jsonPayload) {
            header("nav-call-id", correlationId.toString())
        }.map { it.body.tilYtelser() }
    }

    private fun List<UtbetalingDto>.tilYtelser(): List<Ytelse> {
        val utbetalinger = this.flatMap { it.ytelseListe }
        val relevanteYtelser = utbetalinger.filter { it.ytelsestype == null || it.ytelsestype in relevanteYtelsestyper }
            .groupBy { it.ytelsestype }
        return relevanteYtelser.map { ytelse ->
            Ytelse(
                ytelsetype = ytelse.key?.let { key -> Ytelsetype.entries.find { key == it.tekstverdi } }
                    ?: Ytelsetype.UKJENT,
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
