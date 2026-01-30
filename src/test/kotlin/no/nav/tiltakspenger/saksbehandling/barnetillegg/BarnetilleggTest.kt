package no.nav.tiltakspenger.saksbehandling.barnetillegg

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.uke
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BarnetilleggTest {

    @Nested
    inner class PeriodiserOgFyllUtHullMed0 {
        val bt1 = Pair(2.uke(2026), AntallBarn(1))
        val bt2 = Pair(4.uke(2026), AntallBarn(1))

        @Test
        fun `kaster exception ved overlappende perioder`() {
            val overlappendeBt1 = Pair(Periode(10.januar(2026), 15.januar(2026)), AntallBarn(1))

            assertThrows<IllegalArgumentException> {
                Barnetillegg.periodiserOgFyllUtHullMed0(
                    perioderMedBarn = nonEmptyListOf(bt1, overlappendeBt1),
                    begrunnelse = null,
                    innvilgelsesperioder = nonEmptyListOf(Periode(1.januar(2026), 31.januar(2026))),
                )
            }.message shouldBe ("Støtter ikke overlappende perioder, men var: [1.–4. januar 2026, 5.–11. januar 2026, 10.–15. januar 2026, 16.–31. januar 2026]")
        }

        @Test
        fun `perioder med hull skal bli tettet og bli sammenhengende med en innvilgelsesperiode`() {
            val barnetilleggPerioder = nonEmptyListOf(bt1, bt2)
            val innvilgelsesperiode = Periode(5.januar(2026), 25.januar(2026))

            val sammenhengendeBarnetilleggPerioder = Barnetillegg.periodiserOgFyllUtHullMed0(
                perioderMedBarn = barnetilleggPerioder,
                begrunnelse = null,
                innvilgelsesperioder = nonEmptyListOf(innvilgelsesperiode),
            )

            sammenhengendeBarnetilleggPerioder.periodisering.perioderMedVerdi.let {
                it.size shouldBe 3

                it.first().periode shouldBe 2.uke(2026)
                it.first().verdi shouldBe AntallBarn(1)

                it[1].periode shouldBe 3.uke(2026)
                it[1].verdi shouldBe AntallBarn(0)

                it.last().periode shouldBe 4.uke(2026)
                it.last().verdi shouldBe AntallBarn(1)
            }
        }

        @Test
        fun `perioder med hull skal bli tettet og sammenhengende med to sammenhengende innvilgelsesperioder`() {
            val barnetilleggPerioder = nonEmptyListOf(bt1, bt2)
            val innvilgelsesperioder = nonEmptyListOf(
                Periode(5.januar(2026), 20.januar(2026)),
                Periode(21.januar(2026), 25.januar(2026)),
            )

            val barnetillegg = Barnetillegg.periodiserOgFyllUtHullMed0(
                perioderMedBarn = barnetilleggPerioder,
                begrunnelse = null,
                innvilgelsesperioder = innvilgelsesperioder,
            )

            barnetillegg.periodisering.perioderMedVerdi.let {
                it.size shouldBe 3

                it.first().periode shouldBe 2.uke(2026)
                it.first().verdi shouldBe AntallBarn(1)

                it[1].periode shouldBe 3.uke(2026)
                it[1].verdi shouldBe AntallBarn(0)

                it.last().periode shouldBe 4.uke(2026)
                it.last().verdi shouldBe AntallBarn(1)
            }
        }

        @Test
        fun `perioder med hull skal bli tettet og sammenhengende innenfor ikke-sammenhengende innvilgelsesperioder`() {
            val barnetilleggPerioder = nonEmptyListOf(bt1, bt2)
            val innvilgelsesperioder = nonEmptyListOf(
                Periode(5.januar(2026), 12.januar(2026)),
                Periode(18.januar(2026), 25.januar(2026)),
            )

            val barnetillegg = Barnetillegg.periodiserOgFyllUtHullMed0(
                perioderMedBarn = barnetilleggPerioder,
                begrunnelse = null,
                innvilgelsesperioder = innvilgelsesperioder,
            )

            barnetillegg.periodisering.perioderMedVerdi.let {
                it.size shouldBe 4

                it.first().periode shouldBe 2.uke(2026)
                it.first().verdi shouldBe AntallBarn(1)

                it[1].periode shouldBe Periode(12.januar(2026), 12.januar(2026))
                it[1].verdi shouldBe AntallBarn(0)

                it[2].periode shouldBe Periode(18.januar(2026), 18.januar(2026))
                it[2].verdi shouldBe AntallBarn(0)

                it.last().periode shouldBe 4.uke(2026)
                it.last().verdi shouldBe AntallBarn(1)
            }
        }

        @Test
        fun `skal kaste dersom barnetillegg går utenfor innvilgelsesperiodene`() {
            val barnetilleggPerioder = nonEmptyListOf(bt1, bt2)
            val innvilgelsesperioder = nonEmptyListOf(
                Periode(6.januar(2026), 12.januar(2026)),
                Periode(18.januar(2026), 24.januar(2026)),
            )

            assertThrows<IllegalArgumentException> {
                Barnetillegg.periodiserOgFyllUtHullMed0(
                    perioderMedBarn = barnetilleggPerioder,
                    begrunnelse = null,
                    innvilgelsesperioder = innvilgelsesperioder,
                )
            }.message shouldBe "Barnetilleggsperiodene må være innenfor innvilgelsesperiodene"
        }
    }
}
