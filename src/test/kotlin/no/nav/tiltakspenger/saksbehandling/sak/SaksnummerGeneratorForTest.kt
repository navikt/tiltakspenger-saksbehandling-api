@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.sak

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.dato.januar
import java.time.LocalDate

/**
 * Trådsikker.
 */
class SaksnummerGeneratorForTest(
    første: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", dato = 1.januar(2021)),
) : SaksnummerGenerator {
    private val neste = Atomic(første)

    fun generer(): Saksnummer = neste.getAndUpdate { it.nesteSaksnummer() }

    /** @param dato blir ignorert */
    override fun generer(dato: LocalDate) = generer()
}
