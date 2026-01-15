package no.nav.tiltakspenger.saksbehandling.barnetillegg

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.uke
import no.nav.tiltakspenger.libs.periodisering.Periode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// TODO abn: skriv tester med flere innvilgelsesperioder med og uten hull

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
        fun `perioder med hull skal bli tettet og bli sammenhengende`() {
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
    }
}
