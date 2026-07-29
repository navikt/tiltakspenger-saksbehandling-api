package no.nav.tiltakspenger.saksbehandling.sak

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr

/**
 * Trådsikker.
 * Teller oppover fra [start] og null-padder til 11 siffer, så hvert kall gir et nytt, unikt fnr.
 * Ikke flytt [start] opp mot 999-milliarder; fnr-ene `999999999xx` er reservert av [no.nav.tiltakspenger.saksbehandling.objectmothers.DevSimuleringsscenario].
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
