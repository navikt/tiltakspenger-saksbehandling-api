package no.nav.tiltakspenger.saksbehandling.arenavedtak.infra

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.post
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak
import java.net.URI
import java.time.Clock
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TiltakspengerArenaHttpClient(
    baseUrl: String,
    getToken: suspend () -> AccessToken,
    connectTimeout: Duration = 3.seconds,
    private val timeout: Duration = 6.seconds,
    clock: Clock,
    private val httpKlient: HttpKlient = HttpKlient(clock = clock) {
        this.connectTimeout = connectTimeout
        this.defaultTimeout = timeout
        this.successStatus = { it == 200 }
        this.authTokenProvider = object : AuthTokenProvider {
            override suspend fun hentToken(skipCache: Boolean): AccessToken = getToken()
        }
    },
) : TiltakspengerArenaClient {
    private val uri = URI.create("$baseUrl/azure/tiltakspenger/vedtak")

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
        return httpKlient.post<List<ArenaTPVedtakDto>>(uri, jsonPayload).map {
            it.body.map(ArenaTPVedtakDto::toDomain)
        }
    }
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
