package no.nav.tiltakspenger.saksbehandling.felles

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.backoff.shouldRetry
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.fixedClock
import org.junit.jupiter.api.Test

class ForsøkshistorikkTest {
    @Test
    fun `forrigeForsøk blir alltid nå når man inkrementerer`() {
        var historikk = Forsøkshistorikk.opprett(clock = fixedClock)
        historikk.forrigeForsøk shouldBe null

        repeat(10) { i ->
            historikk = historikk.inkrementer(clock = fixedClock)
            historikk.forrigeForsøk shouldBe nå(fixedClock)
            historikk.antallForsøk shouldBe i + 1L
        }
    }

    @Test
    fun `opprett setter riktige verdier uten forrigeForsøk`() {
        val nå = nå(fixedClock)
        val historikk = Forsøkshistorikk.opprett(clock = fixedClock)

        historikk.forrigeForsøk shouldBe null
        historikk.antallForsøk shouldBe 0
        historikk.nesteForsøk shouldBe nå
    }

    @Test
    fun `opprett setter riktige verdier med forrigeForsøk`() {
        val nå = nå(fixedClock)
        val tidligere = nå.minusHours(1)
        val historikk = Forsøkshistorikk.opprett(forrigeForsøk = tidligere, antallForsøk = 2, clock = fixedClock)
        historikk.forrigeForsøk shouldBe tidligere

        val forventetNeste = tidligere.shouldRetry(2, fixedClock).second
        historikk.nesteForsøk shouldBe forventetNeste
        historikk.antallForsøk shouldBe 2
    }
}
