package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.toNonEmptyListOrThrow
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldeperiodebehandlingerTest {

    @Test
    fun `tillater beregning når alle kjedeider finnes i meldeperioder`() {
        val meldekort = ObjectMother.meldekortUnderBehandling()
        val beregning = ObjectMother.meldekortBeregning(
            meldekortId = meldekort.id,
            startDato = meldekort.fraOgMed,
            kjedeId = meldekort.meldeperioder.first().kjedeId,
        )

        shouldNotThrowAny {
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
        }.message shouldBe "Beregningen må omfatte alle kjedene i behandlingen - Forventet [2025-01-06/2025-01-19], fant []"
    }

    @Test
    fun `tillater beregning som inkluderer perioder utenfor meldeperiodene, f eks ved hull mellom meldeperiodene`() {
        val meldekort = ObjectMother.meldekortUnderBehandling()
        val beregningForBehandlingen = ObjectMother.meldekortBeregning(
            meldekortId = meldekort.id,
            startDato = meldekort.fraOgMed,
            kjedeId = meldekort.meldeperioder.first().kjedeId,
        )
        // Representerer en påfølgende periode som ikke er en del av behandlingen.
        // Dette kan f.eks. være fordi det er et hull mellom meldeperiodene i behandlingen.
        // Eller fordi beregningen er korrigert med sykedager i etterkant.
        val påfølgendePeriode = Periode(LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 2))
        val beregningForPåfølgendePeriode = ObjectMother.meldekortBeregning(
            meldekortId = meldekort.id,
            startDato = påfølgendePeriode.fraOgMed,
        )
        val beregningMedEkstraPeriode = Beregning(
            beregninger = (beregningForBehandlingen.beregninger + beregningForPåfølgendePeriode.beregninger).toNonEmptyListOrThrow(),
            beregningstidspunkt = beregningForBehandlingen.beregningstidspunkt,
        )

        shouldNotThrowAny {
            Meldeperiodebehandlinger(
                meldeperioder = meldekort.meldeperioder.meldeperioder,
                beregning = beregningMedEkstraPeriode,
            )
        }
    }
}
