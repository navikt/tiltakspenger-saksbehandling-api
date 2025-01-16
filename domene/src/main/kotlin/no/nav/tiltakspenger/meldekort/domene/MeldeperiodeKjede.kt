package no.nav.tiltakspenger.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode

data class MeldeperiodeKjede(
    val meldeperioder: NonEmptyList<Meldeperiode>,
) : List<Meldeperiode> by meldeperioder {
    val sakId: SakId = meldeperioder.map { it.sakId }.distinct().single()
    val periode: Periode = meldeperioder.map { it.periode }.distinct().single()
    val id: MeldeperiodeId = meldeperioder.map { it.id }.distinct().single()

    init {
        require(
            meldeperioder.distinctBy { it.hendelseId }.size == meldeperioder.size,
        ) {
            "Meldeperiodekjeden $id har duplikater!"
        }
    }

    fun hentSisteMeldeperiode(): Meldeperiode {
        return this.last()
    }
}
