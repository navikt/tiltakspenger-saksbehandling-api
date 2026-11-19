package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SimuleringHarTidligereUtbetalingTest {

    @Test
    fun `tidligere utbetaling på én dag gjelder hele meldeperioden og simuleringen`() {
        val simulering = ObjectMother.simulering(
            simuleringsdager = nonEmptyListOf(
                simuleringsdag(6.januar(2025), tidligereUtbetalt = 0),
                simuleringsdag(7.januar(2025), tidligereUtbetalt = 298),
            ),
        )

        simulering.simuleringPerMeldeperiode.single().harTidligereUtbetaling shouldBe true
        simulering.harTidligereUtbetaling shouldBe true
    }

    @Test
    fun `ingen tidligere utbetaling når ingen dager er utbetalt tidligere`() {
        val simulering = ObjectMother.simulering(
            simuleringsdager = nonEmptyListOf(simuleringsdag(6.januar(2025), tidligereUtbetalt = 0)),
        )

        simulering.simuleringPerMeldeperiode.single().harTidligereUtbetaling shouldBe false
        simulering.harTidligereUtbetaling shouldBe false
    }

    @Test
    fun `ingen endring har ingen tidligere utbetaling`() {
        Simulering.IngenEndring(simuleringstidspunkt = nå(fixedClock)).harTidligereUtbetaling shouldBe false
    }

    private fun simuleringsdag(dato: LocalDate, tidligereUtbetalt: Int) = Simuleringsdag(
        dato = dato,
        tidligereUtbetalt = tidligereUtbetalt,
        nyUtbetaling = 298,
        totalEtterbetaling = 0,
        totalFeilutbetaling = 0,
        totalTrekk = 0,
        totalJustering = 0,
        totalMotpostering = 0,
        harJustering = false,
    )
}
