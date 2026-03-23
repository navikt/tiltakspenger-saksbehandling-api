package no.nav.tiltakspenger.saksbehandling.beregning.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MeldeperiodeBeregningDbJsonTest {

    @Test
    fun `roundtripper beregningstidspunkt i nytt db-json format`() {
        val beregningstidspunkt = LocalDateTime.of(2025, 5, 1, 12, 30)
        val beregning = ObjectMother.meldekortBeregning().copy(beregningstidspunkt = beregningstidspunkt)

        val actual = beregning.tilBeregningerDbJson()
            .tilBeregningFraMeldekortbehandling(beregning.førsteMeldeperiodeBeregning.meldekortId)

        actual shouldBe beregning
    }

    @Test
    fun `leser legacy db-json uten beregningstidspunkt som null`() {
        val beregning = ObjectMother.meldekortBeregning().copy(
            beregningstidspunkt = LocalDateTime.of(2025, 5, 1, 12, 30),
        )
        val legacyJson = objectMapper.writeValueAsString(
            objectMapper.readTree(beregning.tilBeregningerDbJson()).get("beregninger"),
        )

        val actual = legacyJson.tilBeregningFraMeldekortbehandling(beregning.førsteMeldeperiodeBeregning.meldekortId)

        actual.beregninger shouldBe beregning.beregninger
        actual.beregningstidspunkt shouldBe null
    }
}
