package no.nav.tiltakspenger.common

import arrow.atomic.Atomic
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.sak.SaksnummerGenerator
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
