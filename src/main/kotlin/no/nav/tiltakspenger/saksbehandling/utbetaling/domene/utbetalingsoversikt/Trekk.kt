package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import java.math.BigDecimal

/**
 * Ett trekk i en ytelse, f.eks. skatt eller et utleggstrekk.
 *
 * @param beløp Negativt fra kilden.
 */
data class Trekk(
    val type: String?,
    val beløp: BigDecimal?,
    val kreditor: String?,
)
