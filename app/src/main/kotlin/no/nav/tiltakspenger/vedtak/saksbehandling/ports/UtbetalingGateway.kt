package no.nav.tiltakspenger.vedtak.saksbehandling.ports

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.vedtak.utbetaling.domene.Utbetalingsvedtak

interface UtbetalingGateway {
    suspend fun iverksett(
        vedtak: Utbetalingsvedtak,
        forrigeUtbetalingJson: String?,
        correlationId: CorrelationId,
    ): Either<KunneIkkeUtbetale, SendtUtbetaling>
}

class KunneIkkeUtbetale(
    val request: String? = null,
    val response: String? = null,
    val responseStatus: Int? = null,
)

data class SendtUtbetaling(
    val request: String,
    val response: String,
    val responseStatus: Int,
)
