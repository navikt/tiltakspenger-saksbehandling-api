package no.nav.tiltakspenger.saksbehandling.felles

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.enUkeEtterFixedClock
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class AttesteringerTest {

    @Test
    fun `attesteringene må være sortert i stigende rekkefølge`() {
        val a1 = ObjectMother.underkjentAttestering(clock = fixedClock)
        val a2 = ObjectMother.underkjentAttestering(clock = enUkeEtterFixedClock)

        assertThrows<IllegalArgumentException> {
            Attesteringer(listOf(a2, a1))
        }

        assertDoesNotThrow {
            Attesteringer(listOf(a1, a2))
        }
    }

    @Test
    fun `skal kunne ha max 1 godkjent attestering`() {
        assertThrows<IllegalArgumentException> {
            val a1 = ObjectMother.godkjentAttestering()
            val a2 = ObjectMother.godkjentAttestering()
            Attesteringer(listOf(a1, a2))
        }

        assertDoesNotThrow {
            val a1 = ObjectMother.underkjentAttestering()
            val a2 = ObjectMother.godkjentAttestering()
            Attesteringer(listOf(a1, a2))
        }
    }

    @Test
    fun `legger til en attestering`() {
        val a1 = ObjectMother.underkjentAttestering()
        val a2 = ObjectMother.godkjentAttestering()

        val attesteringer = Attesteringer(listOf(a1))
        val nyeAttesteringer = attesteringer.leggTil(a2)

        nyeAttesteringer.size shouldBe 2
        nyeAttesteringer.first() shouldBe a1
        nyeAttesteringer.last() shouldBe a2
    }

    @Test
    fun `listen med attesteringer er godkjent dersom siste attestering er godkjent`() {
        val a1 = ObjectMother.underkjentAttestering()
        val a2 = ObjectMother.godkjentAttestering()

        val attesteringer = Attesteringer(listOf(a1, a2))

        attesteringer.erGodkjent() shouldBe true
        attesteringer.erUnderkjent() shouldBe false
    }

    @Test
    fun `listen med attesteringer er underkjent dersom siste attestering er underkjent`() {
        val a1 = ObjectMother.underkjentAttestering()
        val a2 = ObjectMother.underkjentAttestering()

        val attesteringer = Attesteringer(listOf(a1, a2))

        attesteringer.erUnderkjent() shouldBe true
        attesteringer.erGodkjent() shouldBe false
    }
}
