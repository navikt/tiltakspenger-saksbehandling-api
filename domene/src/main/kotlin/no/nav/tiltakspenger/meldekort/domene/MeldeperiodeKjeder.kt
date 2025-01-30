package no.nav.tiltakspenger.meldekort.domene

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer

data class MeldeperiodeKjeder(
    private val meldeperiodeKjeder: List<MeldeperiodeKjede>,
) : List<MeldeperiodeKjede> by meldeperiodeKjeder {

    val sakId: SakId? = meldeperiodeKjeder.map { it.sakId }.distinct().singleOrNullOrThrow()
    val saksnummer: Saksnummer? = meldeperiodeKjeder.map { it.saksnummer }.distinct().singleOrNullOrThrow()
    val fnr: Fnr? = meldeperiodeKjeder.map { it.fnr }.distinct().singleOrNullOrThrow()

    init {
        meldeperiodeKjeder.flatten().nonDistinctBy { it.id }.also {
            require(it.isEmpty()) {
                "Meldeperiodekjedene har duplikate meldeperioder - $it"
            }
        }

        meldeperiodeKjeder.zipWithNext { a, b ->
            require(a.periode.fraOgMed <= b.periode.fraOgMed) {
                "Meldeperiodekjedene må være sortert på periode - ${a.id} og ${b.id} var i feil rekkefølge (sak ${a.sakId})"
            }
            require(a.periode.tilOgMed.plusDays(1) == b.periode.fraOgMed) {
                "Meldeperiodekjedene må være sammenhengende - feilet for ${a.id} og ${b.id} (sak ${a.sakId})"
            }
        }
    }

    /** Siste versjon av meldeperiodene */
    val meldeperioder: List<Meldeperiode> get() = this.map { it.last() }

    /**
     * @throws NoSuchElementException hvis det ikke finnes noen meldeperioder
     */
    fun hentSisteMeldeperiode(): Meldeperiode {
        return meldeperiodeKjeder.last().hentSisteMeldeperiode()
    }

    fun hentMeldeperiode(id: HendelseId): Meldeperiode? {
        return meldeperiodeKjeder.asSequence().flatten().find { it.id == id }
    }

    companion object {
        fun fraMeldeperioder(meldeperioder: List<Meldeperiode>): MeldeperiodeKjeder {
            return meldeperioder
                .groupBy { it.meldeperiodeKjedeId }
                .values.mapNotNull { meldeperioderForKjede ->
                    meldeperioderForKjede
                        .sortedBy { it.versjon }
                        .toNonEmptyListOrNull()
                        ?.let { MeldeperiodeKjede(it) }
                }
                .sortedBy { it.periode.fraOgMed }
                .let { MeldeperiodeKjeder(it) }
        }
    }
}
