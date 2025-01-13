package no.nav.tiltakspenger.meldekort.domene

data class MeldeperiodeKjeder(val meldeperiodeKjeder: List<MeldeperiodeKjede>) : List<MeldeperiodeKjede> by meldeperiodeKjeder {
    fun hentSisteMeldeperiod(): Meldeperiode {
        return this.last().hentSisteMeldeperiode()
    }
}
