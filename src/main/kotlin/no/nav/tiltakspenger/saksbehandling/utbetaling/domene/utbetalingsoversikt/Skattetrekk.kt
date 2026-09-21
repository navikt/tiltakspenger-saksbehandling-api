package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import java.math.BigDecimal

/**
 * Ett skattetrekk i en ytelse.
 *
 * @param beløp Negativt fra kilden.
 */
data class Skattetrekk(
    val beløp: BigDecimal?,
)
