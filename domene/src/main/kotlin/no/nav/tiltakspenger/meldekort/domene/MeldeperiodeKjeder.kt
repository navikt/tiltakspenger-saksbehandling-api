package no.nav.tiltakspenger.meldekort.domene

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.periodisering.Periode
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
                "Meldeperiodekjedene må være sortert på periode - ${a.kjedeId} og ${b.kjedeId} var i feil rekkefølge (sak ${a.sakId})"
            }
            require(a.periode.tilOgMed.plusDays(1) == b.periode.fraOgMed) {
                "Meldeperiodekjedene må være sammenhengende - feilet for ${a.kjedeId} og ${b.kjedeId} (sak ${a.sakId})"
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

    fun hentMeldeperiode(id: MeldeperiodeId): Meldeperiode? {
        return meldeperiodeKjeder.asSequence().flatten().find { it.id == id }
    }

    fun hentSisteMeldeperiodeForKjedeId(kjedeId: MeldeperiodeKjedeId): Meldeperiode {
        return meldeperiodeKjeder.single { it.kjedeId == kjedeId }.last()
    }

    fun oppdaterMedNyStansperiode(periode: Periode): Pair<MeldeperiodeKjeder, List<Meldeperiode>> {
        return meldeperiodeKjeder.associate {
            it.oppdaterMedNyStansperiode(periode)
        }.let {
            MeldeperiodeKjeder(it.keys.toList()) to it.values.toList().filterNotNull()
        }
    }

    fun erSisteVersjonAvMeldeperiode(meldeperiode: Meldeperiode): Boolean {
        val meldeperiodeKjede = meldeperiodeKjeder.single { it.kjedeId == meldeperiode.kjedeId }
        return meldeperiode == meldeperiodeKjede.last()
    }

    companion object {
        fun fraMeldeperioder(meldeperioder: List<Meldeperiode>): MeldeperiodeKjeder {
            return meldeperioder
                .groupBy { it.kjedeId }
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
