package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.HendelseId

data class MeldeperiodeKjeder(val meldeperiodeKjeder: List<MeldeperiodeKjede>) : List<MeldeperiodeKjede> by meldeperiodeKjeder {
    fun hentSisteMeldeperiode(): Meldeperiode {
        return this.last().hentSisteMeldeperiode()
    }

    fun hentMeldeperiode(id: HendelseId): Meldeperiode? {
        return this.asSequence().flatten().find { it.hendelseId == id }
    }
}
