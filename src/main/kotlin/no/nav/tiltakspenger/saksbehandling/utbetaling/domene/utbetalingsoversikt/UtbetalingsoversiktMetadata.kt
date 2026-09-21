package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import java.time.LocalDateTime
import kotlin.time.Duration

/**
 * Rå request og response med tidspunkter, lagret for notoritet; systemet leser dem ikke.
 *
 * @param antallForsøk Blir 2 når httpklient har hentet ferskt token etter en 401.
 */
data class UtbetalingsoversiktMetadata(
    val request: String,
    val response: String?,
    val statusKode: Int?,
    val correlationId: CorrelationId,
    val requestSendt: LocalDateTime?,
    val responsMottatt: LocalDateTime?,
    val varighet: Duration,
    val antallForsøk: Int,
)

fun HttpKlientMetadata.tilUtbetalingsoversiktMetadata(correlationId: CorrelationId): UtbetalingsoversiktMetadata =
    UtbetalingsoversiktMetadata(
        request = rawRequestString,
        response = rawResponseString,
        statusKode = statusCode,
        correlationId = correlationId,
        requestSendt = tidsstempler.requestSendt,
        responsMottatt = tidsstempler.responsMottatt,
        varighet = totalDuration,
        antallForsøk = attempts,
    )
