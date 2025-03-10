package no.nav.tiltakspenger.vedtak.clients.poaotilgang

import arrow.core.Either
import arrow.core.getOrElse
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.vedtak.felles.sikkerlogg
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.PoaoTilgangGateway

class PoaoTilgangClient(
    baseUrl: String,
    val getToken: suspend () -> AccessToken,
) : PoaoTilgangGateway {
    private val log = KotlinLogging.logger {}
    private val poaoTilgangclient: PoaoTilgangClient =
        PoaoTilgangHttpClient(
            baseUrl = baseUrl,
            tokenProvider = { runBlocking { getToken().token } },
        )

    override suspend fun erSkjermet(fnr: Fnr, correlationId: CorrelationId): Boolean {
        val token = Either.catch { getToken() }.getOrElse {
            sikkerlogg.error(it) { "Kunne ikke hente token for å hente skjerming. CorrelationId: $correlationId" }
            throw RuntimeException("Kunne ikke hente token for å hente skjerming. Se sikkerlogg for mer kontekst.")
        }
        try {
            val erSkjermet = poaoTilgangclient.erSkjermetPerson(fnr.verdi).getOrThrow()
            return erSkjermet
        } catch (throwable: Throwable) {
            if (throwable is ClientRequestException) {
                val status = throwable.response.status
                if (status == Unauthorized || status == Forbidden) {
                    log.error(RuntimeException("Trigger stacktrace for debug.")) { "Invaliderer cache for systemtoken mot PoaoTilgang. status: $status." }
                    token.invaliderCache()
                }
            }
            if (throwable is IllegalStateException) {
                throw throwable
            } else {
                sikkerlogg.error(throwable) { "Ukjent feil fra Poao Tilgang. CorrelationId: $correlationId" }
                throw RuntimeException("Ukjent feil fra Poao Tilgang, se sikkerlogg for mer kontekst.")
            }
        }
    }
}
