package no.nav.tiltakspenger.utbetaling.domene

import no.nav.tiltakspenger.felles.desember
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.Satsdag
import java.time.LocalDate

class Satser {
    companion object {
        private val satser =
            listOf(
                Sats(Periode(1.januar(2023), 31.desember(2023)), 268, 201, 52, 39),
                Sats(Periode(1.januar(2024), 31.desember(9999)), 285, 214, 53, 40),
            )

        fun sats(dato: LocalDate): Satsdag =
            satser.find { it.fraOgMed <= dato && it.tilOgMed >= dato }?.let { Satsdag(sats = it.sats, satsBarnetillegg = it.satsBarnetillegg, satsRedusert = it.satsRedusert, satsBarnetilleggRedusert = it.satsBarnetilleggRedusert, dato = dato) }
                ?: throw IllegalArgumentException("Fant ingen sats for dato $dato")
    }
}

data class Sats(
    val periode: Periode,
    val sats: Int,
    val satsRedusert: Int,
    val satsBarnetillegg: Int,
    val satsBarnetilleggRedusert: Int,
) {
    val fraOgMed = periode.fraOgMed
    val tilOgMed = periode.tilOgMed

    init {
        require(sats > 0) { "Sats må være positiv, men var $sats" }
        require(satsRedusert > 0) { "Sats redusert må være positiv, men var $satsRedusert" }
        require(satsBarnetillegg > 0) { "Sats barnetillegg må være positiv, men var $satsBarnetillegg" }
        require(satsBarnetilleggRedusert > 0) { "Sats barnetillegg redusert må være, men var $satsBarnetilleggRedusert" }
    }
}
