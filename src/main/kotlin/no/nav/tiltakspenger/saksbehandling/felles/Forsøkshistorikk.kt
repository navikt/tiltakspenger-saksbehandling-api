package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.libs.common.backoff.shouldRetry
import java.time.Clock
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Metadataklasse for å lagre når vi sist prøvde et API-kall.
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

    fun inkrementer(delays: List<Duration> = DEFAULT_DELAY_LIST, clock: Clock): Forsøkshistorikk {
        val nå = LocalDateTime.now(clock)
        val oppdatertAntallForsøk = antallForsøk + 1
        val (_, nesteForsøk) = nå.shouldRetry(
            count = oppdatertAntallForsøk,
            clock = clock,
            delayTable = delays.toDelayTable(),
            maxDelay = delays.last(),
        )

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

        private fun List<Duration>.toDelayTable(): Map<Long, Duration> =
            withIndex().associate { (index, duration) -> index.toLong() + 1 to duration }

        private val DEFAULT_DELAY_LIST: List<Duration> = listOf(
            1.minutes,
            5.minutes,
            15.minutes,
            30.minutes,
            1.hours,
            2.hours,
            4.hours,
            8.hours,
            12.hours,
            24.hours,
        )

        // TODO: ikke bruk map til dette
        private val DEFAULT_DELAY_TABLE: Map<Long, Duration> = DEFAULT_DELAY_LIST.toDelayTable()
    }
}
