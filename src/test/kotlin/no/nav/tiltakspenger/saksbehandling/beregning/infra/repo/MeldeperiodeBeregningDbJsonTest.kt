package no.nav.tiltakspenger.saksbehandling.beregning.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MeldeperiodeBeregningDbJsonTest {

    @Test
    fun `roundtripper beregningstidspunkt i nytt db-json format`() {
        val beregningstidspunkt = LocalDateTime.of(2025, 5, 1, 12, 30)
        val beregning = ObjectMother.meldekortBeregning().copy(beregningstidspunkt = beregningstidspunkt)

        val actual = beregning.tilBeregningerDbJsonString()
            .tilBeregningFraMeldekortbehandling(beregning.førsteMeldeperiodeBeregning.meldekortId)

        actual shouldBe beregning
    }
}
