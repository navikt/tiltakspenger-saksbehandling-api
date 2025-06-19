package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.validerMeldeperiode

@JvmInline
value class AntallDagerForMeldeperiode(val value: Int) : Comparable<AntallDagerForMeldeperiode> {

    override fun compareTo(other: AntallDagerForMeldeperiode): Int {
        return value.compareTo(other.value)
    }

    init {
        // TODO: Dersom du ønsker å bruke denne for stans eller tidslinje, så kan du endre den til >= 0
        require(value > 0 && value <= 14)
    }

    companion object {
        val default: AntallDagerForMeldeperiode = AntallDagerForMeldeperiode(MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE)
    }
}

/**
 * @param periode må være en 14 dagers meldeperiode fra mandag til søndag.
 * @return den høyeste verdien som overlapper perioden eller null dersom ingen overlapper
 */
fun Periodisering<AntallDagerForMeldeperiode>.finnAntallDagerForMeldeperiode(periode: Periode): AntallDagerForMeldeperiode? {
    periode.validerMeldeperiode()
    return this.overlapperMed(periode).map { it.verdi }.maxOfOrNull { it }
}
