package no.nav.tiltakspenger.saksbehandling.felles

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.Level
import java.util.concurrent.atomic.AtomicInteger

/**
 *  Logger med error-level for hvert [n]'te log-kall. Logger ellers med [defaultLevel]
 *
 *  Kan benyttes i de tilfeller der vi kun ønsker å logge feil/få varsler ved gjentatte feil.
 *  */
class ErrorEveryNLogger(
    private val logger: KLogger,
    private val n: Int,
    private val defaultLevel: Level = Level.WARN,
) {
    private val counter = AtomicInteger(0)

    fun log(throwable: Throwable, msg: () -> String) {
        val current = counter.incrementAndGet()

        if (current % n == 0) {
            logger.error(throwable) { msg() }
        } else {
            logger.at(defaultLevel) {
                this.message = msg()
                this.cause = throwable
            }
        }
    }

    fun log(msg: () -> String) {
        val current = counter.incrementAndGet()

        if (current % n == 0) {
            logger.error { msg() }
        } else {
            logger.at(defaultLevel) { msg() }
        }
    }

    fun reset() {
        counter.set(0)
    }

    init {
        require(n > 0) {
            "n må være et positivt tall"
        }

        require(defaultLevel != Level.ERROR) {
            "Ikke bruk denne hvis det alltid skal logges error"
        }
    }
}
