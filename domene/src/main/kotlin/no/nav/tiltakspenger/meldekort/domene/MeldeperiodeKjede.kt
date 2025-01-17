package no.nav.tiltakspenger.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.periodisering.Periode

data class MeldeperiodeKjede(
    private val meldeperioder: NonEmptyList<Meldeperiode>,
) : List<Meldeperiode> by meldeperioder {

    val sakId: SakId = meldeperioder.map { it.sakId }.distinct().single()
    val periode: Periode = meldeperioder.map { it.periode }.distinct().single()
    val id: MeldeperiodeId = meldeperioder.map { it.id }.distinct().single()

    init {
        meldeperioder.nonDistinctBy { it.hendelseId }.also {
            require(it.isEmpty()) {
                "Meldeperiodekjeden $id har duplikate meldeperioder - $it"
            }
        }

        meldeperioder.zipWithNext { a, b ->
            require(a.versjon < b.versjon) {
                "Meldeperiodene må være sortert på versjon - ${a.hendelseId} og ${b.hendelseId} var i feil rekkefølge"
            }
        }
    }

    fun hentSisteMeldeperiode(): Meldeperiode {
        return meldeperioder.last()
    }
}
