package no.nav.tiltakspenger.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer

data class MeldeperiodeKjede(
    private val meldeperioder: NonEmptyList<Meldeperiode>,
) : List<Meldeperiode> by meldeperioder {

    // Disse fungerer også som validering, hvis du fjerner må du legge de inn som init.
    val sakId: SakId = meldeperioder.map { it.sakId }.distinct().single()
    val periode: Periode = meldeperioder.map { it.periode }.distinct().single()
    val saksnummer: Saksnummer = meldeperioder.map { it.saksnummer }.distinct().single()
    val fnr: Fnr = meldeperioder.map { it.fnr }.distinct().single()
    val id: MeldeperiodeKjedeId = meldeperioder.map { it.meldeperiodeKjedeId }.distinct().single()

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
            require(a.hendelseId != b.hendelseId)
        }
    }

    fun hentSisteMeldeperiode(): Meldeperiode {
        return meldeperioder.last()
    }
}
