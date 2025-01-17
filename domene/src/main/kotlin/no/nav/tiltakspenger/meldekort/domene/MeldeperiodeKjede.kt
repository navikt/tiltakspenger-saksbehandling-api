package no.nav.tiltakspenger.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.felles.nonDistinctBy
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode

data class MeldeperiodeKjede(
    private val meldeperioder: NonEmptyList<Meldeperiode>,
) : List<Meldeperiode> by meldeperioder {

    private val meldeperioderSorted = meldeperioder.sortedBy { it.versjon }

    val sakId: SakId = meldeperioder.map { it.sakId }.distinct().single()
    val periode: Periode = meldeperioder.map { it.periode }.distinct().single()
    val id: MeldeperiodeId = meldeperioder.map { it.id }.distinct().single()

    init {
        meldeperioder.nonDistinctBy { it.hendelseId }.also {
            require(it.isEmpty()) {
                "Meldeperiodekjeden $id har duplikate meldeperioder - $it"
            }
        }
    }

    fun hentSisteMeldeperiode(): Meldeperiode {
        return meldeperioderSorted.last()
    }
}
