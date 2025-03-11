package no.nav.tiltakspenger.utbetaling.domene

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.felles.desember
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Sats
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Satser
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SatserTest {
    @Test
    fun `skal returnere korrekt sats for 2023`() {
        val satsdag = Satser.sats(1.januar(2023))
        satsdag.sats shouldBe 268
        satsdag.satsRedusert shouldBe 201
        satsdag.satsBarnetillegg shouldBe 52
        satsdag.satsBarnetilleggRedusert shouldBe 39
    }

    @Test
    fun `skal returnere korrekt sats for 2024`() {
        val satsdag = Satser.sats(1.januar(2024))
        satsdag.sats shouldBe 285
        satsdag.satsRedusert shouldBe 214
        satsdag.satsBarnetillegg shouldBe 53
        satsdag.satsBarnetilleggRedusert shouldBe 40
    }

    @Test
    fun `skal returnere korrekt sats for 2025`() {
        val satsdag = Satser.sats(1.januar(2025))
        satsdag.sats shouldBe 298
        satsdag.satsRedusert shouldBe 224
        satsdag.satsBarnetillegg shouldBe 55
        satsdag.satsBarnetilleggRedusert shouldBe 41
    }

    @Test
    fun `skal kaste exception for dato før 2023`() {
        shouldThrow<IllegalArgumentException> {
            Satser.sats(31.desember(2022))
        }
    }

    @Test
    fun `skal validere at satser er positive ved opprettelse`() {
        shouldThrow<IllegalArgumentException> {
            Sats(
                periode = no.nav.tiltakspenger.libs.periodisering.Periode(
                    fraOgMed = LocalDate.now(),
                    tilOgMed = LocalDate.now(),
                ),
                sats = -1,
                satsRedusert = 100,
                satsBarnetillegg = 100,
                satsBarnetilleggRedusert = 100,
            )
        }

        shouldThrow<IllegalArgumentException> {
            Sats(
                periode = no.nav.tiltakspenger.libs.periodisering.Periode(
                    fraOgMed = LocalDate.now(),
                    tilOgMed = LocalDate.now(),
                ),
                sats = 100,
                satsRedusert = -1,
                satsBarnetillegg = 100,
                satsBarnetilleggRedusert = 100,
            )
        }

        shouldThrow<IllegalArgumentException> {
            Sats(
                periode = no.nav.tiltakspenger.libs.periodisering.Periode(
                    fraOgMed = LocalDate.now(),
                    tilOgMed = LocalDate.now(),
                ),
                sats = 100,
                satsRedusert = 100,
                satsBarnetillegg = -1,
                satsBarnetilleggRedusert = 100,
            )
        }

        shouldThrow<IllegalArgumentException> {
            Sats(
                periode = no.nav.tiltakspenger.libs.periodisering.Periode(
                    fraOgMed = LocalDate.now(),
                    tilOgMed = LocalDate.now(),
                ),
                sats = 100,
                satsRedusert = 100,
                satsBarnetillegg = 100,
                satsBarnetilleggRedusert = -1,
            )
        }
    }
}
