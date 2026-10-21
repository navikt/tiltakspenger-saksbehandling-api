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
