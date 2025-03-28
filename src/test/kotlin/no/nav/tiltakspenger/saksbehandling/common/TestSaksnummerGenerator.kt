@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.common

import arrow.atomic.Atomic
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.sak.SaksnummerGenerator
import java.time.LocalDate

/**
 * Trådsikker. Dersom tester deler database, bør de bruke en felles statisk versjon av denne.
 */
class TestSaksnummerGenerator(
    første: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
) : SaksnummerGenerator {
    private val neste = Atomic(første)

    fun neste(): Saksnummer = neste.getAndUpdate { it.nesteSaksnummer() }

    /** @param dato blir ignorert */
    override fun generer(dato: LocalDate) = neste()
}
