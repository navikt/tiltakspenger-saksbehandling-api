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

fun Satser.Companion.tilSatsHistorikk(periode: Periode): List<SatshistorikkDTO>? {
    if (periode.tilOgMed.year > satser.last().fraOgMed.year) {
        return satser.takeLast(2).map { sats ->
            SatshistorikkDTO(
                år = sats.fraOgMed.year,
                ordinær = sats.sats,
                barnetillegg = sats.satsBarnetillegg,
            )
        }
    } else {
        return null
    }
}
