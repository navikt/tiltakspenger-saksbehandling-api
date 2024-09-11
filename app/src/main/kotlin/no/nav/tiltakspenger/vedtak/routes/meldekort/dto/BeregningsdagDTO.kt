package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.meldekort.domene.Beregningsdag

data class BeregningsdagDTO(
    val beløp: Int,
    val prosent: Int,
)

fun Beregningsdag.toDTO(): BeregningsdagDTO =
    BeregningsdagDTO(beløp = beløp, prosent = prosent)
