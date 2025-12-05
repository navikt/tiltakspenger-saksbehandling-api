package no.nav.tiltakspenger.saksbehandling.felles

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class AttesteringTest {
    @Test
    fun `isGodkjent gir korrekt resultat basert på status`() {
        val godkjentAttestering = ObjectMother.godkjentAttestering()
        val underkjentAttestering = ObjectMother.underkjentAttestering()

        godkjentAttestering.isGodkjent() shouldBe true
        underkjentAttestering.isGodkjent() shouldBe false
    }

    @Test
    fun `isUnderkjent gir korrekt resultat basert på status`() {
        val godkjentAttestering = ObjectMother.godkjentAttestering()
        val underkjentAttestering = ObjectMother.underkjentAttestering()

        godkjentAttestering.isUnderkjent() shouldBe false
        underkjentAttestering.isUnderkjent() shouldBe true
    }
}
