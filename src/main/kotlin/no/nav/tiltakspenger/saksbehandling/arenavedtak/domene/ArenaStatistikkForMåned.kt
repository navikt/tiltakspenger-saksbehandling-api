package no.nav.tiltakspenger.saksbehandling.arenavedtak.domene

import java.time.YearMonth

/**
 * Tiltakspengesaker og vedtak i Arena for én måned, slik tiltakspenger-arena teller dem.
 *
 * @param sakerMedRett Saker med rett til tiltakspenger i minst én dag av måneden.
 * @param nyeRettigheter Vedtak om ny rettighet fattet i måneden.
 * @param gjenopptak Vedtak om gjenopptak fattet i måneden.
 * @param endringer Endringsvedtak fattet i måneden.
 * @param stans Vedtak om stans fattet i måneden.
 */
data class ArenaStatistikkForMåned(
    val måned: YearMonth,
    val sakerMedRett: Int,
    val nyeRettigheter: Int,
    val gjenopptak: Int,
    val endringer: Int,
    val stans: Int,
)
