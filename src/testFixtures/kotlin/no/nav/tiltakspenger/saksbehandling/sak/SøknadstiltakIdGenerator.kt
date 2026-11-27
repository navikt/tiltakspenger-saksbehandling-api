package no.nav.tiltakspenger.saksbehandling.sak

import arrow.atomic.Atomic

/**
 * Trådsikker.
 */
class SøknadstiltakIdGenerator(
    start: Long = 1L,
) {
    private val neste = Atomic(start)

    fun generer(): String {
        val nr = neste.getAndUpdate { it + 1 }
        return "ekstern_tiltaksdeltakelse_id_$nr"
    }
}
