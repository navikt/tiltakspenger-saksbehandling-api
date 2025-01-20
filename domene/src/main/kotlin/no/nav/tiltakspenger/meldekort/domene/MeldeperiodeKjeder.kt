package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer

data class MeldeperiodeKjeder(val meldeperiodeKjeder: List<MeldeperiodeKjede>) : List<MeldeperiodeKjede> by meldeperiodeKjeder {
    fun hentSisteMeldeperiode(): Meldeperiode {
        return this.last().sisteVersjon
    }

    /** Siste versjon av meldeperiodene */
    val meldeperioder: List<Meldeperiode> get() = this.map { it.last() }

    val sakId: SakId? = meldeperiodeKjeder.map { it.sakId }.distinct().singleOrNullOrThrow()
    val saksnummer: Saksnummer? = meldeperiodeKjeder.map { it.saksnummer }.distinct().singleOrNullOrThrow()
    val fnr: Fnr? = meldeperiodeKjeder.map { it.fnr }.distinct().singleOrNullOrThrow()

    init {
        require(
            meldeperiodeKjeder.zipWithNext { a, b ->
                a.periode.tilOgMed.plusDays(1) == b.periode.fraOgMed
            }.all {
                it
            },
        )
    }
}
