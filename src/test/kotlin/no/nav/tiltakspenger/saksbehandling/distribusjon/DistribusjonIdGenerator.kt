package no.nav.tiltakspenger.saksbehandling.distribusjon

import java.util.concurrent.atomic.AtomicLong

/**
 * Trådsikker. Dersom tester deler database, bør de bruke en felles statisk versjon av denne.
 */
class DistribusjonIdGenerator(
    første: Long = 1,
) {
    private val neste = AtomicLong(første)

    fun neste(): DistribusjonId = DistribusjonId(neste.getAndIncrement().toString())
}
