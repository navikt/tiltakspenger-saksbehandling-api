package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import java.time.temporal.ChronoUnit

data class MeldeperiodeKjeder(
    private val meldeperiodeKjeder: List<MeldeperiodeKjede>,
) : List<MeldeperiodeKjede> by meldeperiodeKjeder {
    constructor(meldeperiodeKjede: MeldeperiodeKjede) : this(listOf(meldeperiodeKjede))
    constructor(vararg meldeperiodeKjeder: MeldeperiodeKjede) : this(meldeperiodeKjeder.toList())

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
            require(a.kjedeId != b.kjedeId) {
                "Meldeperiodekjedene kan ikke ha samme kjedeId - ${a.kjedeId} og ${b.kjedeId} (sak ${a.sakId})"
            }
            // 2 kjeder kan ikke ha samme meldeperiode.
            // Og de må være sortert på periode.
            require(a.periode.tilOgMed < b.periode.fraOgMed) {
                "Meldeperiodekjedene må være sortert på periode - ${a.kjedeId} og ${b.kjedeId} var i feil rekkefølge (sak ${a.sakId})"
            }
            require(ChronoUnit.DAYS.between(a.periode.fraOgMed, b.periode.fraOgMed) % 14 == 0L) {
                """
                    Meldeperiodekjedene må følge meldesyklus - feilet for ${a.kjedeId} og ${b.kjedeId} (sak ${a.sakId})
                        Første periode: ${a.periode}.
                        Neste periode: ${b.periode}.
                        Antall dager mellom: ${ChronoUnit.DAYS.between(a.periode.fraOgMed, b.periode.fraOgMed)}.
                """.trimIndent()
            }
        }
    }

    /** Siste versjon av meldeperiodene */
    val sisteMeldeperiodePerKjede: List<Meldeperiode> get() = this.map { it.last() }

    /** Alle versjoner av meldeperiodene */
    val alleMeldeperioder: List<Meldeperiode> get() = this.flatten()

    val meldeperiodeKjederMedRett: List<MeldeperiodeKjede> by lazy {
        this.filter { !it.siste.ingenDagerGirRett }
    }

    /**
     * Henter siste versjon av en meldeperiode.
     * Perioden må matche 1-1 med en meldeperiode.
     */
    fun hentMeldeperiode(periode: Periode): Meldeperiode? {
        return meldeperiodeKjeder.singleOrNullOrThrow {
            it.periode == periode
        }?.hentSisteMeldeperiode()
    }

    /** Henter siste versjon av hver meldeperiode som overlapper med perioden. */
    fun hentMeldeperioderForPeriode(periode: Periode): List<Meldeperiode> {
        return meldeperiodeKjeder.mapNotNull { kjede ->
            if (kjede.periode.overlapperMed(periode)) {
                kjede.hentSisteMeldeperiode()
            } else {
                null
            }
        }
    }

    fun hentMeldeperiodeKjedeForPeriode(periode: Periode): MeldeperiodeKjede? {
        return meldeperiodeKjeder.singleOrNullOrThrow { it.periode == periode }
    }

    /** Perioden må matche 1-1 med en meldeperiode. */
    fun harMeldeperiode(periode: Periode): Boolean = hentMeldeperiode(periode) != null

    fun hentSisteMeldeperiodeForKjedeId(kjedeId: MeldeperiodeKjedeId): Meldeperiode {
        return meldeperiodeKjeder.single { it.kjedeId == kjedeId }.last()
    }

    fun hentSisteMeldeperiodeForKjede(kjedeId: MeldeperiodeKjedeId): Meldeperiode {
        return hentSisteMeldeperiodeForKjedeId(kjedeId)
    }

    fun erSisteVersjonAvMeldeperiode(meldeperiode: Meldeperiode): Boolean {
        val meldeperiodeKjede = meldeperiodeKjeder.single { it.kjedeId == meldeperiode.kjedeId }
        return meldeperiode == meldeperiodeKjede.last()
    }

    fun hentForegåendeMeldeperiodekjede(kjedeId: MeldeperiodeKjedeId): MeldeperiodeKjede? {
        return meldeperiodeKjeder.hentForegående(kjedeId)
    }

    fun hentForegåendeMeldeperiodekjedeMedRett(kjedeId: MeldeperiodeKjedeId): MeldeperiodeKjede? {
        return meldeperiodeKjederMedRett.hentForegående(kjedeId)
    }

    fun hentForMeldeperiodeId(meldeperiodeId: MeldeperiodeId): Meldeperiode? {
        return alleMeldeperioder.singleOrNullOrThrow { it.id == meldeperiodeId }
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

private fun List<MeldeperiodeKjede>.hentForegående(kjedeId: MeldeperiodeKjedeId): MeldeperiodeKjede? {
    this.zipWithNext { a, b -> if (b.kjedeId == kjedeId) return a }
    return null
}
