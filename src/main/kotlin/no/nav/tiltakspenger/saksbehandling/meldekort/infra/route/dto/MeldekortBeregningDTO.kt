package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.beregning.MeldekortBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.BeløpDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.MeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.MeldeperiodeKorrigeringDTO
import no.nav.tiltakspenger.saksbehandling.beregning.infra.dto.tilMeldeperiodeBeregningDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt

data class MeldekortBeregningDTO(
    val totalBeløp: BeløpDTO,
    val beregningForMeldekortetsPeriode: MeldeperiodeBeregningDTO,
    val beregningerForPåfølgendePerioder: List<MeldeperiodeBeregningDTO>,
)

fun MeldekortBeregning.tilMeldekortBeregningDTO(): MeldekortBeregningDTO {
    return MeldekortBeregningDTO(
        totalBeløp = BeløpDTO(
            totalt = totalBeløp,
            ordinært = ordinærBeløp,
            barnetillegg = barnetilleggBeløp,
        ),
        beregningForMeldekortetsPeriode = beregningForMeldekortetsPeriode.tilMeldeperiodeBeregningDTO(),
        beregningerForPåfølgendePerioder = beregningerForPåfølgendePerioder.map { it.tilMeldeperiodeBeregningDTO() },
    )
}

fun MeldekortBehandletManuelt.tilMeldeperiodeKorrigeringDTO(kjedeId: MeldeperiodeKjedeId): MeldeperiodeKorrigeringDTO =
    MeldeperiodeKorrigeringDTO(
        meldekortId = this.id.toString(),
        kjedeId = kjedeId.toString(),
        periode = this.periode.toDTO(),
        iverksatt = this.iverksattTidspunkt,
        beregning = this.beregning.find { it.kjedeId == kjedeId }!!.tilMeldeperiodeBeregningDTO(),
    )
