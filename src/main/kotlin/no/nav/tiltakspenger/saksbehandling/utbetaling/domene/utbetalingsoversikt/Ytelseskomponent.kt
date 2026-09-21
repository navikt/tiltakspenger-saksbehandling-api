package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import java.math.BigDecimal

/**
 * Én linje i beregningen bak en ytelse, f.eks. selve tiltakspengene eller et barnetillegg.
 *
 * @param satsbeløp Kilden nullstiller feltet før det sendes.
 */
data class Ytelseskomponent(
    val type: String?,
    val satsbeløp: BigDecimal?,
    val satstype: String?,
    val satsantall: Double?,
    val beløp: BigDecimal?,
)
