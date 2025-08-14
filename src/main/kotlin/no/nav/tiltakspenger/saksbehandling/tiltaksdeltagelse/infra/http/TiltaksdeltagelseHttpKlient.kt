package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.http

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.http.contentType
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.tiltak.TiltakTilSaksbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Tiltaksdeltagelser
import no.nav.tiltakspenger.saksbehandling.infra.http.httpClientGeneric
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.TiltaksdeltagelseKlient

class TiltaksdeltagelseHttpKlient(
    val baseUrl: String,
    private val getToken: suspend () -> AccessToken,
    engine: HttpClientEngine? = null,
    private val httpClient: HttpClient = httpClientGeneric(engine = engine),
) : TiltaksdeltagelseKlient {
    val log = KotlinLogging.logger {}

    companion object {
        const val NAV_CALL_ID_HEADER = "Nav-Call-Id"
    }

    override suspend fun hentTiltaksdeltagelser(fnr: Fnr, correlationId: CorrelationId): Tiltaksdeltagelser {
        val token = getToken()
        val httpResponse = httpClient.preparePost("$baseUrl/azure/tiltak") {
            header(NAV_CALL_ID_HEADER, correlationId.value)
            bearerAuth(token.token)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(TiltakRequestDTO(fnr.verdi))
        }.execute()
        return when (httpResponse.status) {
            HttpStatusCode.OK -> httpResponse.call.response.body<List<TiltakTilSaksbehandlingDTO>>().let { dto ->
                val relevanteTiltak = dto.filter { it.harFomOgTomEllerRelevantStatus() }
                    .filter { it.rettPaTiltakspenger() }
                Tiltaksdeltagelser(mapTiltak(relevanteTiltak))
            }

            else -> {
                if (httpResponse.status == Unauthorized || httpResponse.status == Forbidden) {
                    log.error(RuntimeException("Trigger stacktrace for debug.")) { "Invaliderer cache for systemtoken mot tiltakspenger-tiltak. status: $httpResponse.status." }
                    token.invaliderCache()
                }
                throw RuntimeException("error (responseCode=${httpResponse.status.value}) from Tiltak")
            }
        }
    }

    private data class TiltakRequestDTO(
        val ident: String,
    )
}
