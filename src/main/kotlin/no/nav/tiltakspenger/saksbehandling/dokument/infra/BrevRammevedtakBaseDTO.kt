@file:Suppress("unused")

package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.periode.Periode

sealed interface BrevRammevedtakBaseDTO {
    val personalia: BrevPersonaliaDTO
    val saksnummer: String
    val saksbehandlerNavn: String
    val beslutterNavn: String?

    // Dette er vår dato, det brukes typisk når bruker klager på vedtaksbrev på dato ...
    val datoForUtsending: String
    val tilleggstekst: String?
    val forhandsvisning: Boolean
    val kontor: String get() = "Nav Tiltakspenger"
}

sealed interface BrevRammevedtakInnvilgelseBaseDTO : BrevRammevedtakBaseDTO {
    @Deprecated("Erstattes av innvilgelsesperioder og barnetillegg - så kan pdfgen få bestemme hvordan de skal presenteres. Datoene blir formatert for å slippe å gjøre det i pdfgen")
    val introTekst: String
    val harBarnetillegg: Boolean
    val satser: List<SatserDTO>
    val innvilgelsesperioder: BrevInnvilgelsesperioderDTO
    val barnetillegg: List<BrevBarnetilleggDTO>

    data class SatserDTO(
        val år: Int,
        val ordinær: Int,
        val barnetillegg: Int,
    )
}

// Periode med datoer formattert for tekst i brev
data class BrevPeriodeDTO private constructor(val fraOgMed: String, val tilOgMed: String) {

    companion object {
        fun fraPeriode(periode: Periode): BrevPeriodeDTO {
            return BrevPeriodeDTO(
                fraOgMed = periode.fraOgMed.format(norskDatoFormatter),
                tilOgMed = periode.tilOgMed.format(norskDatoFormatter),
            )
        }
    }
}

data class BrevInnvilgelsesperioderDTO(
    val antallDagerTekst: String?,
    val perioder: List<BrevPeriodeDTO>,
)

data class BrevBarnetilleggDTO(
    val antallBarnTekst: String,
    val periode: BrevPeriodeDTO,
)
