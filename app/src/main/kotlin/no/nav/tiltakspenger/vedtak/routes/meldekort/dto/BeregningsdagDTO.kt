package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.vedtak.meldekort.domene.Beregningsdag

data class BeregningsdagDTO(
    val beløp: Int,
    val prosent: Int,
    val barnetillegg: Int,
)

fun Beregningsdag.toDTO(): BeregningsdagDTO =
    BeregningsdagDTO(beløp = beløp, prosent = prosent, barnetillegg = beløpBarnetillegg)
