package no.nav.tiltakspenger.meldekort.domene

data class MeldeperiodeKjeder(val meldeperiodeKjeder: List<MeldeperiodeKjede>) : List<MeldeperiodeKjede> by meldeperiodeKjeder {
    fun hentSisteMeldeperiod(): Meldeperiode {
        return this.last().hentSisteMeldeperiode()
    }

    /** Siste versjon av meldeperiodene */
    val meldeperioder: List<Meldeperiode> get() = this.map { it.last() }
}
