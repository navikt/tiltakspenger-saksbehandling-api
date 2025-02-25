package no.nav.tiltakspenger.barnetillegg

import no.nav.tiltakspenger.libs.periodisering.Periodisering
import java.time.LocalDate

/**
 * Representerer en periodisering av barnetillegg.
 */
data class Barnetillegg(
    val value: Periodisering<Int>,
) {
    init {
        require(value.all { it.verdi >= 0 && it.verdi < 100 }) { "Barnetillegg må være et tall mellom 0 og 99" }
    }

    /**
     * @return 0 dersom datoen er utenfor periodiseringen.
     */
    fun antallBarnPåDato(dato: LocalDate): Int {
        return value.hentVerdiForDag(dato) ?: 0
    }
}
