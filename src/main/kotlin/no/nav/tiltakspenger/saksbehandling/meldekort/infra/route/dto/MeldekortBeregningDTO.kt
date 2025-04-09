package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlet
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregning

data class MeldekortBeregningDTO(
    val totalBeløp: BeløpDTO,
    val beregningForMeldekortetsPeriode: MeldeperiodeBeregningDTO,
    val beregningerForPåfølgendePerioder: List<MeldeperiodeBeregningDTO>,
)

data class MeldeperiodeKorrigeringDTO(
    val meldekortId: String,
    val kjedeId: String,
    val periode: PeriodeDTO,
    val beregning: MeldeperiodeBeregningDTO,
)

data class MeldeperiodeBeregningDTO(
    val beløp: BeløpDTO,
    val dager: List<MeldeperiodeBeregningDagDTO>,
)

data class BeløpDTO(
    val totalt: Int,
    val ordinært: Int,
    val barnetillegg: Int,
)

fun MeldekortBeregning.tilMeldekortBeregningDTO(): MeldekortBeregningDTO {
    return MeldekortBeregningDTO(
        totalBeløp = BeløpDTO(
            totalt = beregnTotaltBeløp(),
            ordinært = beregnTotalOrdinærBeløp(),
            barnetillegg = beregnTotalBarnetillegg(),
        ),
        beregningForMeldekortetsPeriode = beregningForMeldekortetsPeriode.tilMeldeperiodeBeregningDTO(),
        beregningerForPåfølgendePerioder = beregningerForPåfølgendePerioder.map { it.tilMeldeperiodeBeregningDTO() },
    )
}

fun MeldeperiodeBeregning.tilMeldeperiodeBeregningDTO(): MeldeperiodeBeregningDTO {
    return MeldeperiodeBeregningDTO(
        beløp = BeløpDTO(
            totalt = beregnTotaltBeløp(),
            ordinært = beregnTotalOrdinærBeløp(),
            barnetillegg = beregnTotalBarnetillegg(),
        ),
        dager = this.tilMeldeperiodeBeregningDagerDTO(),
    )
}

fun MeldekortBehandlet.tilMeldeperiodeKorrigeringDTO(kjedeId: MeldeperiodeKjedeId): MeldeperiodeKorrigeringDTO =
    MeldeperiodeKorrigeringDTO(
        meldekortId = id.toString(),
        kjedeId = kjedeId.toString(),
        periode = periode.toDTO(),
        beregning = this.beregning.find { it.kjedeId == kjedeId }!!.tilMeldeperiodeBeregningDTO(),
    )
