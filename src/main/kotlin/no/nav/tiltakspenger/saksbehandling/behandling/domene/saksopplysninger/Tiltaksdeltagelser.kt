package no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import kotlin.collections.ArrayList

/**
 * 0, 1 eller flere tiltaksdeltagelser knyttet til en person.
 * Kan være på tvers av kildesystemer (Arena, Komet, Team Tiltak)
 */
data class Tiltaksdeltagelser(
    val value: List<Tiltaksdeltagelse>,
) : List<Tiltaksdeltagelse> by value {

    constructor(value: Tiltaksdeltagelse) : this(listOf(value))

    init {
        value.map { it.eksternDeltagelseId }.also {
            require(it.size == it.distinct().size) {
                "eksternDeltagelseId kan ikke ha duplikate verdier, men hadde: $it"
            }
        }
    }

    val tidligsteFraOgMed by lazy { value.mapNotNull { it.deltagelseFraOgMed }.minOrNull() }
    val senesteTilOgMed by lazy { value.mapNotNull { it.deltagelseTilOgMed }.maxOrNull() }

    /**
     * Denne ignorerer mellomrom mellom tiltaksdeltagelsene.
     * Velger tidligste fraOgMed og seneste tilOgMed.
     * Null dersom enten [tidligsteFraOgMed] eller [senesteTilOgMed] er null.
     */
    val totalPeriode: Periode? by lazy {
        if (tidligsteFraOgMed == null || senesteTilOgMed == null) {
            null
        } else {
            Periode(tidligsteFraOgMed!!, senesteTilOgMed!!)
        }
    }

    fun getTiltaksdeltagelse(eksternDeltagelseId: String): Tiltaksdeltagelse? {
        return this.find { it.eksternDeltagelseId == eksternDeltagelseId }
    }

    /** Filtrerer bort tiltaksdeltagelser med ufullstendige perioder */
    val perioder by lazy { value.mapNotNull { it.periode } }

    fun filtrerPåTiltaksdeltagelsesIDer(tiltaksdeltagelseIder: List<String>): Tiltaksdeltagelser {
        return this.filter { it.eksternDeltagelseId in tiltaksdeltagelseIder }
    }

    inline fun filter(predicate: (Tiltaksdeltagelse) -> Boolean): Tiltaksdeltagelser {
        return Tiltaksdeltagelser(filterTo(ArrayList(), predicate))
    }

    /**
     * Inkluderer tvilstilfellene der vi ikke kan si med sikkherhet om de overlapper eller ikke.
     * Se [no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse.overlapperMed] for mer informasjon.
     */
    fun overlappende(tiltaksdeltagelse: Tiltaksdeltagelse): Tiltaksdeltagelser {
        return this.filter { it.overlapperMed(tiltaksdeltagelse) ?: true }
    }

    /**
     * Inkluderer tvilstilfellene der vi ikke kan si med sikkherhet om de overlapper eller ikke.
     * Se [no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse.overlapperMed] for mer informasjon.
     */
    fun overlappende(tiltaksdeltagelser: Tiltaksdeltagelser): Tiltaksdeltagelser {
        return this.filter { deltagelse ->
            tiltaksdeltagelser.any { it.overlapperMed(deltagelse) ?: true }
        }
    }

    companion object {
        fun empty() = Tiltaksdeltagelser(emptyList())
    }
}
