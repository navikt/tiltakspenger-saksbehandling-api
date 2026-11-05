package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.satser.Satser
import no.nav.tiltakspenger.saksbehandling.dokument.infra.BrevRammevedtakInnvilgelseBaseDTO.SatserDTO

fun Satser.Companion.tilSatserDTO(periode: Periode): List<SatserDTO> {
    return satser.filter { it.periode.overlapperMed(periode) }.map {
        SatserDTO(
            år = it.periode.fraOgMed.year,
            ordinær = it.sats,
            barnetillegg = it.satsBarnetillegg,
        )
    }
}

/**
 * Året den siste kjente satsen trådte i kraft.
 * pdfgenrs bruker det til å si fra når beløp etter dette året er vist med satsene for dette året.
 */
fun Satser.Companion.sisteKjenteSatsår(): Int = satser.last().fraOgMed.year
