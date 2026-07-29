package no.nav.tiltakspenger.saksbehandling.felles

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class AttesteringerTest {

    @Test
    fun `attesteringene må være sortert i stigende rekkefølge`() {
        val clock = TikkendeKlokke()
        val a1 = ObjectMother.underkjentAttestering(clock = clock)
        val a2 = ObjectMother.underkjentAttestering(clock = clock)

        shouldThrow<IllegalArgumentException> {
            Attesteringer(listOf(a2, a1))
        }

        shouldNotThrowAny {
            Attesteringer(listOf(a1, a2))
        }
    }

    @Test
    fun `skal kunne ha max 1 godkjent attestering`() {
        val clock = TikkendeKlokke()
        shouldThrow<IllegalArgumentException> {
            val a1 = ObjectMother.godkjentAttestering(clock = clock)
            val a2 = ObjectMother.godkjentAttestering(clock = clock)
            Attesteringer(listOf(a1, a2))
        }

        shouldNotThrowAny {
            val a1 = ObjectMother.underkjentAttestering(clock = clock)
            val a2 = ObjectMother.godkjentAttestering(clock = clock)
            Attesteringer(listOf(a1, a2))
        }
    }

    @Test
    fun `legger til en attestering`() {
        val clock = TikkendeKlokke()
        val a1 = ObjectMother.underkjentAttestering(clock = clock)
        val a2 = ObjectMother.godkjentAttestering(clock = clock)

        val attesteringer = Attesteringer(listOf(a1))
        val nyeAttesteringer = attesteringer.leggTil(a2)

        nyeAttesteringer.size shouldBe 2
        nyeAttesteringer.first() shouldBe a1
        nyeAttesteringer.last() shouldBe a2
    }

    @Test
    fun `listen med attesteringer er godkjent dersom siste attestering er godkjent`() {
        val clock = TikkendeKlokke()
        val a1 = ObjectMother.underkjentAttestering(clock = clock)
        val a2 = ObjectMother.godkjentAttestering(clock = clock)

        val attesteringer = Attesteringer(listOf(a1, a2))

        attesteringer.erGodkjent() shouldBe true
        attesteringer.erUnderkjent() shouldBe false
    }

    @Test
    fun `listen med attesteringer er underkjent dersom siste attestering er underkjent`() {
        val clock = TikkendeKlokke()
        val a1 = ObjectMother.underkjentAttestering(clock = clock)
        val a2 = ObjectMother.underkjentAttestering(clock = clock)

        val attesteringer = Attesteringer(listOf(a1, a2))

        attesteringer.erUnderkjent() shouldBe true
        attesteringer.erGodkjent() shouldBe false
    }
}
