package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.libs.common.backoff.shouldRetry
import java.time.Clock
import java.time.LocalDateTime

/**
 * Metadataklasse for å lagre når vi siste prøvde et API-kall.
 * Brukes for debug, logging eller for å begrense antall forsøk.
 */
data class Forsøkshistorikk(
    val forrigeForsøk: LocalDateTime?,
    val nesteForsøk: LocalDateTime,
    val antallForsøk: Long,
) {
    init {
        require(antallForsøk > -1) { "antallForsøk kan ikke være negativ, men var $antallForsøk" }
    }

    fun inkrementer(clock: Clock): Forsøkshistorikk {
        val nå = LocalDateTime.now(clock)
        val forrigeForsøk = this.forrigeForsøk ?: nå
        val oppdatertAntallForsøk = antallForsøk + 1
        val nesteForsøk = forrigeForsøk.shouldRetry(oppdatertAntallForsøk, clock).second
        return Forsøkshistorikk(
            forrigeForsøk = forrigeForsøk,
            antallForsøk = oppdatertAntallForsøk,
            nesteForsøk = nesteForsøk,
        )
    }

    companion object {
        fun opprett(
            forrigeForsøk: LocalDateTime? = null,
            antallForsøk: Long = 0,
            clock: Clock,
        ): Forsøkshistorikk {
            val nå = LocalDateTime.now(clock)
            val forrigeForsøk = forrigeForsøk
            val nesteForsøk = forrigeForsøk?.shouldRetry(antallForsøk, clock)?.second ?: nå
            return Forsøkshistorikk(
                forrigeForsøk = forrigeForsøk,
                nesteForsøk = nesteForsøk,
                antallForsøk = antallForsøk,
            )
        }
    }
}
