package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.beregning.Beregningsdag

data class BeregningsdagDTO(
    val beløp: Int,
    val prosent: Int,
    val barnetillegg: Int,
)

fun Beregningsdag.toBeregningsdagDTO(): BeregningsdagDTO =
    BeregningsdagDTO(beløp = beløp, prosent = prosent, barnetillegg = beløpBarnetillegg)
