package no.nav.tiltakspenger.saksbehandling.sak

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr

/**
 * Trådsikker.
 */
class FnrGenerator(
    start: Long = 0L,
) {
    private val neste = Atomic(start)

    fun generer(): Fnr {
        val nr = neste.getAndUpdate { it + 1 }
        return Fnr.fromString(nr.toString().padStart(11, '0'))
    }
}
