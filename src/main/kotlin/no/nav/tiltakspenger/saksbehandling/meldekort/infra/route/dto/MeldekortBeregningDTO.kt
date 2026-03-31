package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.BeløpDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.MeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.MeldeperiodeKorrigeringDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.tilMeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell

data class MeldekortBeregningDTO(
    val totalBeløp: BeløpDTO,
    val beregningForMeldekortetsPeriode: MeldeperiodeBeregningDTO,
    val beregningerForPåfølgendePerioder: List<MeldeperiodeBeregningDTO>,
    val beregningstidspunkt: String?,
)

fun Beregning.tilMeldekortBeregningDTO(): MeldekortBeregningDTO {
    return MeldekortBeregningDTO(
        totalBeløp = BeløpDTO(
            totalt = totalBeløp,
            ordinært = ordinærBeløp,
            barnetillegg = barnetilleggBeløp,
        ),
        beregningForMeldekortetsPeriode = førsteMeldeperiodeBeregning.tilMeldeperiodeBeregningDTO(),
        beregningerForPåfølgendePerioder = beregningerForPåfølgendePerioder.map { it.tilMeldeperiodeBeregningDTO() },
        beregningstidspunkt = beregningstidspunkt.toString(),
    )
}

fun MeldekortbehandlingManuell.tilMeldeperiodeKorrigeringDTO(kjedeId: MeldeperiodeKjedeId): MeldeperiodeKorrigeringDTO =
    MeldeperiodeKorrigeringDTO(
        meldekortId = this.id.toString(),
        kjedeId = kjedeId.toString(),
        periode = this.periode.toDTO(),
        iverksatt = this.iverksattTidspunkt,
        beregning = this.beregning.find { it.kjedeId == kjedeId }!!.tilMeldeperiodeBeregningDTO(),
    )
