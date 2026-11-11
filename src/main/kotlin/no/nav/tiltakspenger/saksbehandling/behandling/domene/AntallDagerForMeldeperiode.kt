package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.validerMeldeperiode

const val MAKS_DAGER_FOR_MELDEPERIODE: Int = 14

@JvmInline
value class AntallDagerForMeldeperiode(val value: Int) : Comparable<AntallDagerForMeldeperiode> {

    override fun compareTo(other: AntallDagerForMeldeperiode): Int {
        return value.compareTo(other.value)
    }

    init {
        require(value in 1..MAKS_DAGER_FOR_MELDEPERIODE)
    }

    companion object {
        val default: AntallDagerForMeldeperiode =
            AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE)
    }
}

/**
 * @param periode må være en 14 dagers meldeperiode fra mandag til søndag.
 * @return den høyeste verdien som overlapper perioden eller null dersom ingen overlapper
 */
fun Periodisering<AntallDagerForMeldeperiode>.finnAntallDagerForMeldeperiode(periode: Periode): AntallDagerForMeldeperiode? {
    periode.validerMeldeperiode()
    return this.overlappendePeriode(periode).perioderMedVerdi.map { it.verdi }.maxOfOrNull { it }
}

/**
 * Summerer antall dager for tiltaksdeltakelser som er innvilget i samme periode.
 * Summen begrenses til antall dager i en meldeperiode.
 */
fun Iterable<AntallDagerForMeldeperiode>.summer(): AntallDagerForMeldeperiode {
    return AntallDagerForMeldeperiode(minOf(sumOf { it.value }, MAKS_DAGER_FOR_MELDEPERIODE))
}
