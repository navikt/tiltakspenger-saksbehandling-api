package no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import kotlin.collections.ArrayList

/**
 * 0, 1 eller flere tiltaksdeltakelser knyttet til en person.
 * Kan være på tvers av kildesystemer (Arena, Komet, Team Tiltak)
 */
data class Tiltaksdeltakelser(
    val value: List<Tiltaksdeltakelse>,
) : List<Tiltaksdeltakelse> by value {

    constructor(value: Tiltaksdeltakelse) : this(listOf(value))

    init {
        value.map { it.eksternDeltakelseId }.also {
            require(it.size == it.distinct().size) {
                "eksternDeltakelseId kan ikke ha duplikate verdier, men hadde: $it"
            }
        }
    }

    val tidligsteFraOgMed by lazy { value.mapNotNull { it.deltakelseFraOgMed }.minOrNull() }
    val senesteTilOgMed by lazy { value.mapNotNull { it.deltakelseTilOgMed }.maxOrNull() }

    /**
     * Denne ignorerer mellomrom mellom tiltaksdeltakelsene.
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

    fun getTiltaksdeltakelse(eksternDeltakelseId: String): Tiltaksdeltakelse? {
        return this.find { it.eksternDeltakelseId == eksternDeltakelseId }
    }

    /** Filtrerer bort tiltaksdeltakelser med ufullstendige perioder */
    val perioder by lazy { value.mapNotNull { it.periode } }

    inline fun filter(predicate: (Tiltaksdeltakelse) -> Boolean): Tiltaksdeltakelser {
        return Tiltaksdeltakelser(filterTo(ArrayList(), predicate))
    }

    companion object {
        fun empty() = Tiltaksdeltakelser(emptyList())
    }
}
