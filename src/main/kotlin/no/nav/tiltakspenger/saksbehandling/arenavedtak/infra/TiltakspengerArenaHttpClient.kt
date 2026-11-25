package no.nav.tiltakspenger.saksbehandling.arenavedtak.infra

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.UriSynlighet
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.SerialisertJson
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaSisteDagMedRettEtter
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaStatistikkForMåned
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaStatistikkForSisteDagMedRett
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaUtbetalingerForMåned
import java.math.BigDecimal
import java.net.URI
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Klient mot tiltakspenger-arena for å hente tiltakspengevedtak og statistikk fra Arena.
 *
 * Kildekode: https://github.com/navikt/tiltakspenger-arena
 * Dokumentasjon: README-en i kildekode-repoet
 * API-spec: -
 * Slack: #tiltakspenger-værsågod (eget team)
 * Teamkatalog: https://teamkatalogen.nav.no/team/15bca3d2-2584-4167-85ba-faab1f1cfb53
 *
 * Statistikkallene har egen klient med [statistikkTimeout], fordi per-maned har brukt 59 sekunder for ett år.
 * 180 sekunder gir rundt tre ganger margin og ligger under tidsavbruddet på 300 sekunder i ingressen til tiltakspenger-arena, så klienten gir opp før ingressen.
 * Jobben som kaller dem, går hver time og er ikke brukerrettet, så ventetiden koster ingenting.
 */
class TiltakspengerArenaHttpClient(
    baseUrl: String,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 3.seconds,
    timeout: Duration = 6.seconds,
    statistikkTimeout: Duration = 180.seconds,
    clock: Clock,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : TiltakspengerArenaClient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
        ),
        transport = transport,
    )

    private val statistikkKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = statistikkTimeout,
            auth = KlientAuth.System(authTokenProvider),
            uriSynlighet = UriSynlighet.VanligLogg,
        ),
        transport = transport,
    )

    private val uri = URI.create("$baseUrl/azure/tiltakspenger/vedtak")
    private val statistikkUri = "$baseUrl/azure/tiltakspenger/statistikk"

    override suspend fun hentTiltakspengevedtakFraArena(
        fnr: Fnr,
        periode: Periode,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, List<ArenaTPVedtak>> {
        val jsonPayload = serialize(
            VedtakRequest(
                ident = fnr.verdi,
                fom = periode.fraOgMed,
                tom = periode.tilOgMed,
            ),
        )
        return httpKlient.postJson<List<ArenaTPVedtakDto>>(
            uri = uri,
            body = SerialisertJson(jsonPayload),
            godta = Statusregel.Eksakt(200),
        ).map {
            it.body.map(ArenaTPVedtakDto::toDomain)
        }
    }

    override suspend fun hentStatistikkPerMåned(år: Int): Either<HttpKlientError, List<ArenaStatistikkForMåned>> {
        return statistikkKlient.getJson<List<StatistikkForMånedDto>>(
            uri = URI.create("$statistikkUri/per-maned?aar=$år"),
            godta = Statusregel.Eksakt(200),
        ).map { svar -> svar.body.map { it.toDomain() } }
    }

    override suspend fun hentSisteDagMedRettEtter(etter: LocalDate): Either<HttpKlientError, ArenaSisteDagMedRettEtter> {
        return statistikkKlient.getJson<SisteDagMedRettEtterDto>(
            uri = URI.create("$statistikkUri/siste-dag-med-rett?etter=$etter"),
            godta = Statusregel.Eksakt(200),
        ).map { svar -> svar.body.toDomain() }
    }

    override suspend fun hentUtbetalingerPerMåned(år: Int): Either<HttpKlientError, List<ArenaUtbetalingerForMåned>> {
        return statistikkKlient.getJson<List<UtbetalingerForMånedDto>>(
            uri = URI.create("$statistikkUri/utbetalinger-per-maned?aar=$år"),
            godta = Statusregel.Eksakt(200),
        ).map { svar -> svar.body.map { it.toDomain() } }
    }
}

private data class StatistikkForMånedDto(
    val måned: String,
    val sakerMedRett: Int,
    val nyeRettigheter: Int,
    val gjenopptak: Int,
    val endringer: Int,
    val stans: Int,
) {
    fun toDomain() = ArenaStatistikkForMåned(
        måned = YearMonth.parse(måned),
        sakerMedRett = sakerMedRett,
        nyeRettigheter = nyeRettigheter,
        gjenopptak = gjenopptak,
        endringer = endringer,
        stans = stans,
    )
}

private data class SisteDagMedRettEtterDto(
    val etter: LocalDate,
    val perSisteDagMedRett: List<StatistikkForSisteDagMedRettDto>,
) {
    fun toDomain() = ArenaSisteDagMedRettEtter(
        etter = etter,
        perSisteDagMedRett = perSisteDagMedRett.map { it.toDomain() },
    )
}

private data class StatistikkForSisteDagMedRettDto(
    val sisteDagMedRett: LocalDate?,
    val saker: Int,
    val personer: Int,
) {
    fun toDomain() = ArenaStatistikkForSisteDagMedRett(sisteDagMedRett = sisteDagMedRett, saker = saker, personer = personer)
}

private data class UtbetalingerForMånedDto(
    val måned: String,
    val personerMedUtbetaling: Int,
    val dagerUtbetalt: BigDecimal,
    val beløpGrunnsats: BigDecimal,
    val beløpBarnetillegg: BigDecimal,
    val personerMedBeregnetIkkeUtbetalt: Int,
    val beløpBeregnetIkkeUtbetalt: BigDecimal,
) {
    fun toDomain() = ArenaUtbetalingerForMåned(
        måned = YearMonth.parse(måned),
        personerMedUtbetaling = personerMedUtbetaling,
        dagerUtbetalt = dagerUtbetalt,
        beløpGrunnsats = beløpGrunnsats,
        beløpBarnetillegg = beløpBarnetillegg,
        personerMedBeregnetIkkeUtbetalt = personerMedBeregnetIkkeUtbetalt,
        beløpBeregnetIkkeUtbetalt = beløpBeregnetIkkeUtbetalt,
    )
}

/** Kun ment brukt av testene utenfor denne fila. */
data class ArenaTPVedtakDto(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val rettighet: ArenaTPVedtak.Rettighet,
    val vedtakId: Long,
) {
    fun toDomain(): ArenaTPVedtak = ArenaTPVedtak(
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
        rettighet = rettighet,
        vedtakId = vedtakId,
    )
}

private data class VedtakRequest(
    val ident: String,
    val fom: LocalDate?,
    val tom: LocalDate?,
)
