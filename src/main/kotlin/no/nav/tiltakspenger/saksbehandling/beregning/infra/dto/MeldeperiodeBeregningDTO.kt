package no.nav.tiltakspenger.saksbehandling.beregning.infra.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import java.time.LocalDateTime

data class MeldeperiodeKorrigeringDTO(
    val meldekortId: String,
    val kjedeId: String,
    val periode: PeriodeDTO,
    val iverksatt: LocalDateTime?,
    val beregning: MeldeperiodeBeregningDTO,
)

data class MeldeperiodeBeregningDTO(
    val kjedeId: String,
    val periode: PeriodeDTO,
    val beløp: BeløpDTO,
    val dager: List<MeldeperiodeBeregningDagDTO>,
)

data class BeløpDTO(
    val totalt: Int,
    val ordinært: Int,
    val barnetillegg: Int,
)

fun MeldeperiodeBeregning.tilMeldeperiodeBeregningDTO(): MeldeperiodeBeregningDTO {
    return MeldeperiodeBeregningDTO(
        kjedeId = this.kjedeId.toString(),
        periode = this.periode.toDTO(),
        beløp = BeløpDTO(
            totalt = totalBeløp,
            ordinært = ordinærBeløp,
            barnetillegg = barnetilleggBeløp,
        ),
        dager = this.tilMeldeperiodeBeregningDagerDTO(),
    )
}
