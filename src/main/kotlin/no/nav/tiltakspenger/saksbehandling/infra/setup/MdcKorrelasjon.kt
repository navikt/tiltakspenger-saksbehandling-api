package no.nav.tiltakspenger.saksbehandling.infra.setup

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.withCorrelationIdSuspend
import no.nav.tiltakspenger.saksbehandling.felles.Korrelasjon

/**
 * Legger correlation-id-en i MDC under [mdcKey], samme nøkkel som Ktor bruker for call-id i HTTP-forespørsler.
 * Id-en ligger i coroutine-konteksten og fjernes når blokken er ferdig, se [withCorrelationIdSuspend].
 */
class MdcKorrelasjon(
    private val mdcKey: String = CALL_ID_MDC_KEY,
) : Korrelasjon {
    override suspend fun <T> medNyCorrelationId(block: suspend (CorrelationId) -> T): T {
        var resultat: Result<T>? = null
        withCorrelationIdSuspend(mdcKey) { correlationId ->
            resultat = Result.success(block(correlationId))
        }
        return resultat!!.getOrThrow()
    }
}
