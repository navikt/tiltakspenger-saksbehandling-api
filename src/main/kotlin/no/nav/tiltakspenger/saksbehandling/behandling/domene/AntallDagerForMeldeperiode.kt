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
 * Kaster dersom summen er mer enn antall dager i en meldeperiode, se [overstigerMaksForMeldeperiode].
 */
fun Iterable<AntallDagerForMeldeperiode>.summer(): AntallDagerForMeldeperiode {
    val sum = sumOf { it.value }
    require(sum <= MAKS_DAGER_FOR_MELDEPERIODE) {
        "Summen av antall dager per meldeperiode ($sum) kan ikke være mer enn $MAKS_DAGER_FOR_MELDEPERIODE"
    }
    return AntallDagerForMeldeperiode(sum)
}

/** Om summen av antall dager er mer enn antall dager i en meldeperiode. */
fun Iterable<AntallDagerForMeldeperiode>.overstigerMaksForMeldeperiode(): Boolean {
    return sumOf { it.value } > MAKS_DAGER_FOR_MELDEPERIODE
}
