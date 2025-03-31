package no.nav.tiltakspenger.saksbehandling.felles

import java.time.Clock
import java.time.LocalDateTime

/**
 * Metadataklasse for å lagre når vi siste prøvde et API-kall.
 * Brukes for debug, logging eller for å begrense antall forsøk.
 */
data class Forsøkshistorikk(
    val forrigeForsøk: LocalDateTime,
    val antallForsøk: Long,
) {
    init {
        require(antallForsøk > 0) { "antallForsøk må være større enn 0, men var $antallForsøk" }
    }

    fun inkrementer(clock: Clock): Forsøkshistorikk {
        return Forsøkshistorikk(
            forrigeForsøk = LocalDateTime.now(clock),
            antallForsøk = antallForsøk + 1,
        )
    }

    companion object {
        fun førsteForsøk(clock: Clock): Forsøkshistorikk {
            return Forsøkshistorikk(
                forrigeForsøk = LocalDateTime.now(clock),
                antallForsøk = 1,
            )
        }
    }
}
