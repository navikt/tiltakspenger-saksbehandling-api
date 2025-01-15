package no.nav.tiltakspenger.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode

data class MeldeperiodeKjede(
    val meldeperioder: NonEmptyList<Meldeperiode>,
) : List<Meldeperiode> by meldeperioder {
    val sakId: SakId = meldeperioder.map { it.sakId }.distinct().single()
    val periode: Periode = meldeperioder.map { it.periode }.distinct().single()

    fun hentSisteMeldeperiode(): Meldeperiode {
        return this.last()
    }

    fun hentMeldeperiode(id: HendelseId): Meldeperiode? {
        return this.find { it.hendelseId == id }
    }
}
