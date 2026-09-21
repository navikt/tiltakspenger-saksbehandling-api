package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import org.junit.jupiter.api.Test
import java.time.Duration

/** 5. mai 2025 er en mandag, 9. mai en fredag, og 1. mai er en fast helligdag på en torsdag. */
class OppslagsplanTest {
    private val mandagKl12 = 5.mai(2025).atTime(12, 0)

    @Test
    fun `sak med nylig sendt utbetaling slås opp når økonomisystemet åpner neste åpne dag`() {
        Oppslagsplan.etterVellykketOppslag(mandagKl12, harNyligSendtUtbetaling = true) shouldBe
            Oppslagsplan(nesteOppslag = 6.mai(2025).atTime(6, 10), antallFeilPåRad = 0)
    }

    @Test
    fun `helg og fast helligdag hoppes over`() {
        Oppslagsplan.etterVellykketOppslag(9.mai(2025).atTime(7, 5), harNyligSendtUtbetaling = true).nesteOppslag shouldBe
            12.mai(2025).atTime(6, 10)
        Oppslagsplan.etterVellykketOppslag(30.april(2025).atTime(7, 5), harNyligSendtUtbetaling = true).nesteOppslag shouldBe
            2.mai(2025).atTime(6, 10)
    }

    @Test
    fun `sak uten nylig sendt utbetaling slås opp om 30 dager`() {
        Oppslagsplan.etterVellykketOppslag(mandagKl12, harNyligSendtUtbetaling = false) shouldBe
            Oppslagsplan(nesteOppslag = 4.juni(2025).atTime(12, 0), antallFeilPåRad = 0)
    }

    @Test
    fun `ventetiden etter feil øker med antall feil på rad`() {
        val ventetider = (0..7).map { tidligereFeilPåRad ->
            val plan = Oppslagsplan.etterFeiletOppslag(mandagKl12, tidligereFeilPåRad, clock = fixedClock)
            plan.antallFeilPåRad shouldBe tidligereFeilPåRad + 1
            Duration.between(mandagKl12, plan.nesteOppslag).toMinutes()
        }

        ventetider shouldBe listOf<Long>(1, 5, 15, 30, 60, 120, 240, 480)
    }

    @Test
    fun `nytt forsøk etter feil legges aldri utenfor åpningstiden`() {
        Oppslagsplan.etterFeiletOppslag(mandagKl12, tidligereFeilPåRad = 8, clock = fixedClock).nesteOppslag shouldBe 6.mai(2025).atTime(6, 10)
        Oppslagsplan.etterFeiletOppslag(mandagKl12, tidligereFeilPåRad = 9, clock = fixedClock).nesteOppslag shouldBe 6.mai(2025).atTime(12, 0)
        Oppslagsplan.etterFeiletOppslag(mandagKl12, tidligereFeilPåRad = 20, clock = fixedClock).nesteOppslag shouldBe 6.mai(2025).atTime(12, 0)
        Oppslagsplan.etterFeiletOppslag(5.mai(2025).atTime(20, 49), tidligereFeilPåRad = 0, clock = fixedClock).nesteOppslag shouldBe 6.mai(2025).atTime(6, 10)
        Oppslagsplan.etterFeiletOppslag(9.mai(2025).atTime(20, 0), tidligereFeilPåRad = 4, clock = fixedClock).nesteOppslag shouldBe 12.mai(2025).atTime(6, 10)
    }

    @Test
    fun `antall feil på rad kan ikke være negativt`() {
        shouldThrow<IllegalArgumentException> { Oppslagsplan(nesteOppslag = mandagKl12, antallFeilPåRad = -1) }
        shouldThrow<IllegalArgumentException> { Oppslagsplan.etterFeiletOppslag(mandagKl12, tidligereFeilPåRad = -1, clock = fixedClock) }
    }
}
