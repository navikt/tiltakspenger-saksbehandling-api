package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.libs.common.backoff.shouldRetry
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.fixedClock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

class ForsøkshistorikkTest {
    @Test
    fun `forrigeForsøk blir alltid nå når man inkrementerer`() {
        var historikk = Forsøkshistorikk.opprett(clock = fixedClock)
        assertNull(historikk.forrigeForsøk)

        repeat(10) { i ->
            historikk = historikk.inkrementer(fixedClock)
            assertEquals(nå(fixedClock), historikk.forrigeForsøk)
            assertEquals(i + 1L, historikk.antallForsøk)
        }
    }

    @Test
    fun `opprett setter riktige verdier uten forrigeForsøk`() {
        val nå = nå(fixedClock)
        val historikk = Forsøkshistorikk.opprett(clock = fixedClock)

        assertNull(historikk.forrigeForsøk)
        assertEquals(0, historikk.antallForsøk)
        assertEquals(nå, historikk.nesteForsøk)
    }

    @Test
    fun `opprett setter riktige verdier med forrigeForsøk`() {
        val nå = nå(fixedClock)
        val tidligere = nå.minusHours(1)
        val historikk = Forsøkshistorikk.opprett(forrigeForsøk = tidligere, antallForsøk = 2, clock = fixedClock)
        assertEquals(tidligere, historikk.forrigeForsøk)

        val forventetNeste = tidligere.shouldRetry(2, fixedClock).second
        assertEquals(forventetNeste, historikk.nesteForsøk)
        assertEquals(2, historikk.antallForsøk)
    }
}
