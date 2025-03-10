package no.nav.tiltakspenger.fakes.clients

import arrow.atomic.Atomic
import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.KunneIkkeUtbetale
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.SendtUtbetaling
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.UtbetalingGateway
import no.nav.tiltakspenger.vedtak.utbetaling.domene.Utbetalingsvedtak

class UtbetalingFakeGateway : UtbetalingGateway {
    private val utbetalinger = Atomic(mutableMapOf<VedtakId, Utbetaling>())

    override suspend fun iverksett(
        vedtak: Utbetalingsvedtak,
        forrigeUtbetalingJson: String?,
        correlationId: CorrelationId,
    ): Either<KunneIkkeUtbetale, SendtUtbetaling> {
        val response = SendtUtbetaling("request - ${vedtak.id}", "response - ${vedtak.id}", responseStatus = 202)
        val utbetaling = Utbetaling(vedtak, correlationId, response)
        utbetalinger.get()[vedtak.id] = utbetaling
        return response.right()
    }

    data class Utbetaling(
        val vedtak: Utbetalingsvedtak,
        val correlationId: CorrelationId,
        val sendtUtbetaling: SendtUtbetaling,
    )
}
