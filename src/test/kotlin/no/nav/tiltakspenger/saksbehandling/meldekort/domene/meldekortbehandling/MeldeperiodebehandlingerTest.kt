package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class MeldeperiodebehandlingerTest {

    @Test
    fun `tillater beregning når alle kjedeider finnes i meldeperioder`() {
        val meldekort = ObjectMother.meldekortUnderBehandling()
        val beregning = ObjectMother.meldekortBeregning(
            meldekortId = meldekort.id,
            startDato = meldekort.fraOgMed,
            kjedeId = meldekort.kjedeIdLegacy,
        )

        assertDoesNotThrow {
            Meldeperiodebehandlinger(
                meldeperioder = meldekort.meldeperioder.meldeperioder,
                beregning = beregning,
            )
        }
    }

    @Test
    fun `kaster hvis beregning mangler kjedeid fra meldeperioder`() {
        val meldekort = ObjectMother.meldekortUnderBehandling()
        val annenPeriode = Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 2))
        val beregningMedAnnenKjede = ObjectMother.meldekortBeregning(
            meldekortId = meldekort.id,
            startDato = annenPeriode.fraOgMed,
        )

        shouldThrow<IllegalArgumentException> {
            Meldeperiodebehandlinger(
                meldeperioder = meldekort.meldeperioder.meldeperioder,
                beregning = beregningMedAnnenKjede,
            )
        }.message shouldBe "Beregningen må omfatte alle kjedene i behandlingen - Forventet [2025-01-06/2025-01-19], fant [2025-01-20/2025-02-02]"
    }
}
