package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import no.nav.tiltakspenger.libs.periodisering.Periode

data class TiltaksdeltakelserFraRegister(
    val value: List<TiltaksdeltakelseFraRegister>,
) : List<TiltaksdeltakelseFraRegister> by value {

    constructor(value: TiltaksdeltakelseFraRegister) : this(listOf(value))

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

    fun getTiltaksdeltakelse(eksternDeltakelseId: String): TiltaksdeltakelseFraRegister? {
        return this.find { it.eksternDeltakelseId == eksternDeltakelseId }
    }

    /** Filtrerer bort tiltaksdeltakelser med ufullstendige perioder */
    val perioder by lazy { value.mapNotNull { it.periode } }

    fun filtrerPåTiltaksdeltakelsesIDer(tiltaksdeltakelseIder: List<String>): TiltaksdeltakelserFraRegister {
        return this.filter { it.eksternDeltakelseId in tiltaksdeltakelseIder }
    }

    inline fun filter(predicate: (TiltaksdeltakelseFraRegister) -> Boolean): TiltaksdeltakelserFraRegister {
        return TiltaksdeltakelserFraRegister(filterTo(ArrayList(), predicate))
    }

    /**
     * Inkluderer tvilstilfellene der vi ikke kan si med sikkherhet om de overlapper eller ikke.
     * Se [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse.overlapperMed] for mer informasjon.
     */
    fun overlappende(tiltaksdeltakelser: TiltaksdeltakelserFraRegister): TiltaksdeltakelserFraRegister {
        return this.filter { deltakelse ->
            tiltaksdeltakelser.any { it.overlapperMed(deltakelse) ?: true }
        }
    }

    companion object {
        fun empty() = TiltaksdeltakelserFraRegister(emptyList())
    }
}
