package no.nav.tiltakspenger.saksbehandling.omgjøring

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

data class Omgjøringsperioder(
    val omgjøringsperiode: List<Omgjøringsperiode>,
) : List<Omgjøringsperiode> by omgjøringsperiode {
    val rammevedtakIDer: List<VedtakId> by lazy { map { it.rammevedtakId }.distinct().sorted() }
    val perioder: List<Periode> by lazy { map { it.periode } }
    val fraOgMed: LocalDate? by lazy { minByOrNull { it.periode.fraOgMed }?.periode?.fraOgMed }
    val tilOgMed: LocalDate? by lazy { maxByOrNull { it.periode.tilOgMed }?.periode?.tilOgMed }
    val totalPeriode = if (fraOgMed != null) Periode(fraOgMed!!, tilOgMed!!) else null

    companion object {
        val empty: Omgjøringsperioder = Omgjøringsperioder(emptyList())

        fun create(omgjørRammevedtak: Rammevedtak): Omgjøringsperioder {
            return Omgjøringsperioder(
                listOf(
                    Omgjøringsperiode(
                        rammevedtakId = omgjørRammevedtak.id,
                        periode = omgjørRammevedtak.periode,
                        omgjøringsgrad = Omgjøringsgrad.HELT,
                    ),
                ),
            )
        }
    }

    init {
        require((fraOgMed == null && tilOgMed == null) || (fraOgMed != null && tilOgMed != null)) {
            "fraOgMed og tilOgMed må enten begge være null eller begge være satt"
        }
        require(this.map { it.periode }.sortedBy { it.fraOgMed } == this.map { it.periode }) {
            "Omgjøringsperioder må være sortert på fraOgMed dato og periodene kan ikke overlappe."
        }
        this
            .map { it.periode }
            .sortedBy { it.fraOgMed }
            .zipWithNext { a, b ->
                require(!a.overlapperMed(b)) {
                    "Omgjøringsperioder kan ikke overlappe. Fant overlappende perioder: $a og $b"
                }
            }
    }
}
