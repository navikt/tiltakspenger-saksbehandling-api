package no.nav.tiltakspenger.meldekort.domene

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.HendelseId

data class MeldeperiodeKjeder(val meldeperiodeKjeder: List<MeldeperiodeKjede>) : List<MeldeperiodeKjede> by meldeperiodeKjeder {

    init {
        require(meldeperiodeKjeder.flatten().distinctBy { it.hendelseId }.size == meldeperiodeKjeder.size) {
            "Meldeperiodekjedene har duplikater!"
        }
    }

    fun hentSisteMeldeperiode(): Meldeperiode {
        return meldeperiodeKjeder.last().hentSisteMeldeperiode()
    }

    fun hentMeldeperiode(id: HendelseId): Meldeperiode? {
        return meldeperiodeKjeder.asSequence().flatten().find { it.hendelseId == id }
    }

    companion object {
        fun fraMeldeperioder(meldeperioder: List<Meldeperiode>): MeldeperiodeKjeder {
            return meldeperioder
                .groupBy { it.id }
                .values.mapNotNull { meldeperioderForKjede ->
                    meldeperioderForKjede.toNonEmptyListOrNull()?.let { MeldeperiodeKjede(it) }
                }
                .let { MeldeperiodeKjeder(it) }
        }
    }
}
