package no.nav.tiltakspenger.saksbehandling.arenavedtak.domene

import java.math.BigDecimal
import java.time.YearMonth

/**
 * Utbetalte tiltakspenger i Arena for én måned, slik `tiltakspenger-arena` teller dem.
 *
 * @param personerMedUtbetaling Unike personer med netto utbetalt grunnsats over null i måneden.
 * @param dagerUtbetalt Utbetalte dager med grunnsats.
 * @param beløpGrunnsats Utbetalt grunnsats.
 * @param beløpBarnetillegg Utbetalt barnetillegg.
 * @param personerMedBeregnetIkkeUtbetalt Unike personer med beregnet, men ikke utbetalt grunnsats over null i måneden.
 * @param beløpBeregnetIkkeUtbetalt Beregnet, men ikke utbetalt grunnsats og barnetillegg.
 */
data class ArenaUtbetalingerForMåned(
    val måned: YearMonth,
    val personerMedUtbetaling: Int,
    val dagerUtbetalt: BigDecimal,
    val beløpGrunnsats: BigDecimal,
    val beløpBarnetillegg: BigDecimal,
    val personerMedBeregnetIkkeUtbetalt: Int,
    val beløpBeregnetIkkeUtbetalt: BigDecimal,
)
