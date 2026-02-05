package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.libs.common.backoff.shouldRetry
import java.time.Clock
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

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
        val oppdatertAntallForsøk = antallForsøk + 1
        val nesteForsøk = nå.shouldRetry(oppdatertAntallForsøk, clock).second
        return Forsøkshistorikk(
            forrigeForsøk = nå,
            antallForsøk = oppdatertAntallForsøk,
            nesteForsøk = nesteForsøk,
        )
    }

    companion object {
        fun opprett(
            forrigeForsøk: LocalDateTime? = null,
            antallForsøk: Long = 0,
            delayTable: Map<Long, Duration> = DEFAULT_DELAY_TABLE,
            clock: Clock,
        ): Forsøkshistorikk {
            val nå = LocalDateTime.now(clock)
            val forrigeForsøk = forrigeForsøk
            val nesteForsøk = forrigeForsøk?.shouldRetry(antallForsøk, clock, delayTable)?.second ?: nå
            return Forsøkshistorikk(
                forrigeForsøk = forrigeForsøk,
                nesteForsøk = nesteForsøk,
                antallForsøk = antallForsøk,
            )
        }

        val DEFAULT_DELAY_TABLE: Map<Long, Duration> = mapOf(
            1L to 1.minutes,
            2L to 5.minutes,
            3L to 15.minutes,
            4L to 30.minutes,
            5L to 1.hours,
            6L to 2.hours,
            7L to 4.hours,
            8L to 8.hours,
            9L to 12.hours,
            10L to 24.hours,
        )
    }
}
