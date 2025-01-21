package no.nav.tiltakspenger.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer

data class MeldeperiodeKjede(
    val meldeperioder: NonEmptyList<Meldeperiode>,
) : List<Meldeperiode> by meldeperioder {
    // Disse fungerer også som validering, hvis du fjerner må du legge de inn som init.
    val sakId: SakId = meldeperioder.map { it.sakId }.distinct().single()
    val periode: Periode = meldeperioder.map { it.periode }.distinct().single()
    val saksnummer: Saksnummer = meldeperioder.map { it.saksnummer }.distinct().single()
    val fnr: Fnr = meldeperioder.map { it.fnr }.distinct().single()
    val id: MeldeperiodeId = meldeperioder.map { it.id }.distinct().single()

    val sisteVersjon: Meldeperiode = last()

    init {
        meldeperioder.zipWithNext { a, b ->
            require(a.versjon.inc() == b.versjon)
            require(a.opprettet.isBefore(b.opprettet))
            require(a.hendelseId != b.hendelseId)
        }
    }
}
