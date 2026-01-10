@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.sak

import arrow.atomic.Atomic
import java.time.Clock
import java.time.LocalDate

/**
 * Trådsikker.
 */
class TestSaksnummerGenerator(
    første: Saksnummer,
) : SaksnummerGenerator {
    private val neste = Atomic(første)

    fun neste(): Saksnummer = neste.getAndUpdate { it.nesteSaksnummer() }

    /** @param dato blir ignorert */
    override fun generer(dato: LocalDate) = neste()
}
