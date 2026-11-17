package no.nav.tiltakspenger.saksbehandling.felles

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import org.junit.jupiter.api.Test

class LocalDateExTest {

    @Test
    fun `faste helligdager er helligdager`() {
        listOf(1.januar(2025), 1.mai(2025), 17.mai(2025), 24.desember(2025), 25.desember(2025), 26.desember(2025), 31.desember(2025))
            .forEach { it.erHelligdag() shouldBe true }
    }

    /** 1. påskedag var 31. mars 2024 og 20. april 2025, og er 5. april 2026. */
    @Test
    fun `skjærtorsdag, langfredag, 2 påskedag, Kristi himmelfartsdag og 2 pinsedag er helligdager`() {
        listOf(28.mars(2024), 29.mars(2024), 1.april(2024), 9.mai(2024), 20.mai(2024)).forEach { it.erHelligdag() shouldBe true }
        listOf(17.april(2025), 18.april(2025), 21.april(2025), 29.mai(2025), 9.juni(2025)).forEach { it.erHelligdag() shouldBe true }
        listOf(2.april(2026), 3.april(2026), 6.april(2026), 14.mai(2026), 25.mai(2026)).forEach { it.erHelligdag() shouldBe true }
    }

    @Test
    fun `dagene rundt de bevegelige helligdagene er ikke helligdager`() {
        listOf(16.april(2025), 19.april(2025), 22.april(2025), 28.mai(2025), 30.mai(2025), 10.juni(2025))
            .forEach { it.erHelligdag() shouldBe false }
    }
}
