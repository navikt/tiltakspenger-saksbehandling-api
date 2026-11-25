package no.nav.tiltakspenger.saksbehandling.arenavedtak.domene

import java.time.LocalDate

/**
 * Tiltakspengesaker og personer i Arena med rett minst én dag etter [etter], fordelt på siste dag med rett, slik tiltakspenger-arena teller dem.
 * Saker og personer med rett etter en senere dato X er summen over `sisteDagMedRett > X`, der `null` alltid er med.
 */
data class ArenaSisteDagMedRettEtter(
    val etter: LocalDate,
    val perSisteDagMedRett: List<ArenaStatistikkForSisteDagMedRett>,
) {
    companion object {
        /** Tidligste dato fordelingen gjelder for; tall for alle senere datoer kan regnes ut fra den. */
        val startdato: LocalDate = LocalDate.of(2024, 1, 1)
    }
}

/**
 * @param sisteDagMedRett `null` betyr at retten ikke har sluttdato.
 * @param saker Saker der dette er siste dag med rett.
 * @param personer Personer der dette er seneste siste dag med rett på tvers av sakene.
 */
data class ArenaStatistikkForSisteDagMedRett(
    val sisteDagMedRett: LocalDate?,
    val saker: Int,
    val personer: Int,
)
