package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKorrigering
import java.time.LocalDateTime

data class MeldeperiodeKorrigeringDTO(
    val meldekortId: String,
    val kjedeId: String,
    val periode: PeriodeDTO,
    val iverksatt: LocalDateTime,
    val dager: List<MeldeperiodeBeregningDagDTO>,
)

fun MeldeperiodeKorrigering.tilDTO(): MeldeperiodeKorrigeringDTO = MeldeperiodeKorrigeringDTO(
    meldekortId = this.meldekortId.toString(),
    kjedeId = this.kjedeId.toString(),
    periode = periode.toDTO(),
    iverksatt = this.iverksatt,
    dager = this.dager.toMeldeperiodeBeregningDagerDTO(),
)
