package no.nav.tiltakspenger.saksbehandling.barnetillegg

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juli
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.dato.oktober
import no.nav.tiltakspenger.libs.dato.september
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetilleggMedIdent
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetilleggUtenIdent
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.random.Random

/**
 * Speiler testene til `periodiserBarnetilleggFraSøknad` i frontend, med samme periode og fødselsdatoer.
 * I tillegg testes grensetilfellene rundt periodens start og slutt, skuddår og at resultatet alltid dekker hele perioden.
 */
class PeriodiserBarnetilleggFraSøknadTest {

    private val periode = 1.juli(2024) til 30.juni(2025)

    private fun barn(
        fødselsdato: LocalDate,
        oppholderSegIEØS: Søknad.JaNeiSpm = Søknad.JaNeiSpm.Ja,
    ): BarnetilleggFraSøknad = barnetilleggMedIdent(fødselsdato = fødselsdato, oppholderSegIEØS = oppholderSegIEØS)

    private fun List<BarnetilleggFraSøknad>.periodiser(periode: Periode = this@PeriodiserBarnetilleggFraSøknadTest.periode) =
        periodiserAntallBarn(periode).perioderMedVerdi.toList()

    private fun forventet(vararg perioder: Pair<Periode, Int>): List<PeriodeMedVerdi<AntallBarn>> =
        perioder.map { (periode, antall) -> PeriodeMedVerdi(AntallBarn(antall), periode) }

    @Nested
    inner class SammeTilfellerSomFrontend {
        @Test
        fun `tre barn under 16 i hele perioden gir tre barn i hele perioden`() {
            listOf(
                barn(1.januar(2010)),
                barn(15.august(2012)),
                barn(1.januar(2020)),
            ).periodiser() shouldBe forventet(periode to 3)
        }

        @Test
        fun `barn født midt i perioden gir barnetillegg fra og med fødselsdatoen`() {
            listOf(barn(1.august(2024))).periodiser() shouldBe forventet(
                (1.juli(2024) til 31.juli(2024)) to 0,
                (1.august(2024) til 30.juni(2025)) to 1,
            )
        }

        @Test
        fun `barn som fyller 16 i perioden gir barnetillegg til og med dagen før 16-årsdagen`() {
            listOf(barn(31.desember(2008))).periodiser() shouldBe forventet(
                (1.juli(2024) til 30.desember(2024)) to 1,
                (31.desember(2024) til 30.juni(2025)) to 0,
            )
        }

        @Test
        fun `kombinasjon av fødsler og barn som fyller 16 gir riktig antall i hver delperiode`() {
            listOf(
                barn(15.juli(2008)),
                barn(31.desember(2008)),
                barn(15.juni(2009)),
                barn(1.januar(2010)),
                barn(1.august(2024)),
                barn(1.januar(2025)),
                barn(15.juni(2025)),
            ).periodiser() shouldBe forventet(
                (1.juli(2024) til 14.juli(2024)) to 4,
                (15.juli(2024) til 31.juli(2024)) to 3,
                (1.august(2024) til 30.desember(2024)) to 4,
                (31.desember(2024) til 31.desember(2024)) to 3,
                // Barnet som fyller 16 og barnet som blir født i juni 2025 går opp i opp, så antallet er uendret.
                (1.januar(2025) til 30.juni(2025)) to 4,
            )
        }

        @Test
        fun `barn som fylte 16 før perioden gir ikke barnetillegg`() {
            listOf(barn(30.juni(2008))).periodiser() shouldBe forventet(periode to 0)
        }

        @Test
        fun `barn som ikke oppholder seg i EØS regnes ikke med`() {
            listOf(
                barn(1.januar(2010)),
                barn(1.januar(2012), oppholderSegIEØS = Søknad.JaNeiSpm.Nei),
            ).periodiser() shouldBe forventet(periode to 1)
        }

        @Test
        fun `hull uten barn mellom to barn gir 0 barn i hullet`() {
            // Frontend utelater hullet, mens vi fyller det med 0 barn slik at hele perioden er dekket.
            listOf(
                barn(1.september(2008)),
                barn(1.mars(2025)),
            ).periodiser() shouldBe forventet(
                (1.juli(2024) til 31.august(2024)) to 1,
                (1.september(2024) til 28.februar(2025)) to 0,
                (1.mars(2025) til 30.juni(2025)) to 1,
            )
        }
    }

    @Nested
    inner class Grensetilfeller {
        @Test
        fun `ingen barn gir 0 barn i hele perioden`() {
            emptyList<BarnetilleggFraSøknad>().periodiser() shouldBe forventet(periode to 0)
        }

        @Test
        fun `barn som ikke har besvart spørsmålet om opphold regnes ikke med`() {
            listOf(barn(1.januar(2010), oppholderSegIEØS = Søknad.JaNeiSpm.IkkeBesvart)).periodiser() shouldBe
                forventet(periode to 0)
        }

        @Test
        fun `ingen barn i EØS gir 0 barn i hele perioden`() {
            listOf(
                barn(1.januar(2010), oppholderSegIEØS = Søknad.JaNeiSpm.Nei),
                barn(1.januar(2012), oppholderSegIEØS = Søknad.JaNeiSpm.IkkeBesvart),
            ).periodiser() shouldBe forventet(periode to 0)
        }

        @Test
        fun `barn registrert manuelt regnes med på samme måte som barn fra PDL`() {
            listOf(
                barn(1.januar(2010)),
                barnetilleggUtenIdent(fødselsdato = 1.august(2024)),
            ).periodiser() shouldBe forventet(
                (1.juli(2024) til 31.juli(2024)) to 1,
                (1.august(2024) til 30.juni(2025)) to 2,
            )
        }

        @Test
        fun `barn født før perioden gir barnetillegg fra periodens start`() {
            listOf(barn(1.januar(2020))).periodiser() shouldBe forventet(periode to 1)
        }

        @Test
        fun `barn født på første dag i perioden gir barnetillegg i hele perioden`() {
            listOf(barn(1.juli(2024))).periodiser() shouldBe forventet(periode to 1)
        }

        @Test
        fun `barn født på siste dag i perioden gir barnetillegg kun siste dag`() {
            listOf(barn(30.juni(2025))).periodiser() shouldBe forventet(
                (1.juli(2024) til 29.juni(2025)) to 0,
                (30.juni(2025) til 30.juni(2025)) to 1,
            )
        }

        @Test
        fun `barn født etter perioden gir ikke barnetillegg`() {
            listOf(barn(1.juli(2025))).periodiser() shouldBe forventet(periode to 0)
        }

        @Test
        fun `barn som fyller 16 på første dag i perioden gir ikke barnetillegg`() {
            listOf(barn(1.juli(2008))).periodiser() shouldBe forventet(periode to 0)
        }

        @Test
        fun `barn som fyller 16 på andre dag i perioden gir barnetillegg kun første dag`() {
            listOf(barn(2.juli(2008))).periodiser() shouldBe forventet(
                (1.juli(2024) til 1.juli(2024)) to 1,
                (2.juli(2024) til 30.juni(2025)) to 0,
            )
        }

        @Test
        fun `barn som fyller 16 på siste dag i perioden gir barnetillegg til og med nest siste dag`() {
            listOf(barn(30.juni(2009))).periodiser() shouldBe forventet(
                (1.juli(2024) til 29.juni(2025)) to 1,
                (30.juni(2025) til 30.juni(2025)) to 0,
            )
        }

        @Test
        fun `barn som fyller 16 dagen etter perioden gir barnetillegg i hele perioden`() {
            listOf(barn(1.juli(2009))).periodiser() shouldBe forventet(periode to 1)
        }

        @Test
        fun `tvillinger teller som to barn`() {
            listOf(barn(1.august(2024)), barn(1.august(2024))).periodiser() shouldBe forventet(
                (1.juli(2024) til 31.juli(2024)) to 0,
                (1.august(2024) til 30.juni(2025)) to 2,
            )
        }

        @Test
        fun `et barn som fyller 16 samme dag som et annet blir født gir uendret antall uten periodeskifte`() {
            listOf(barn(1.oktober(2008)), barn(1.oktober(2024))).periodiser() shouldBe forventet(periode to 1)
        }

        @Test
        fun `barn som fyller 16 og barn som blir født på samme dag i hver sin retning gir riktig antall`() {
            listOf(
                barn(1.januar(2012)),
                barn(1.april(2009)),
                barn(1.april(2025)),
                barn(1.april(2025)),
            ).periodiser() shouldBe forventet(
                (1.juli(2024) til 31.mars(2025)) to 2,
                (1.april(2025) til 30.juni(2025)) to 3,
            )
        }

        @Test
        fun `rekkefølgen på barna påvirker ikke resultatet`() {
            val barna = listOf(
                barn(15.juli(2008)),
                barn(1.august(2024)),
                barn(31.desember(2008)),
                barn(1.januar(2025)),
            )
            barna.reversed().periodiser() shouldBe barna.periodiser()
            barna.shuffled(Random(42)).periodiser() shouldBe barna.periodiser()
        }

        @Test
        fun `periode på én dag gir én periode`() {
            val dag = 1.juli(2024) til 1.juli(2024)
            listOf(barn(1.januar(2010))).periodiser(dag) shouldBe forventet(dag to 1)
            listOf(barn(1.juli(2008))).periodiser(dag) shouldBe forventet(dag to 0)
            listOf(barn(1.juli(2024))).periodiser(dag) shouldBe forventet(dag to 1)
        }

        @Test
        fun `barn født 29 februar fyller 16 på 29 februar når 16-årsdagen er i et skuddår`() {
            listOf(barn(29.februar(2008))).periodiser(1.januar(2024) til 31.desember(2024)) shouldBe forventet(
                (1.januar(2024) til 28.februar(2024)) to 1,
                (29.februar(2024) til 31.desember(2024)) to 0,
            )
        }

        @Test
        fun `barn født 29 februar fyller 16 på 28 februar når 16-årsdagen ikke er i et skuddår`() {
            // 2100 er ikke et skuddår, siden det er delelig med 100 men ikke med 400.
            listOf(barn(29.februar(2084))).periodiser(1.januar(2100) til 31.desember(2100)) shouldBe forventet(
                (1.januar(2100) til 27.februar(2100)) to 1,
                (28.februar(2100) til 31.desember(2100)) to 0,
            )
        }

        @Test
        fun `resultatet stemmer med under16ForDato for hver dag i perioden`() {
            val barna = listOf(
                barn(29.februar(2008)),
                barn(15.juli(2008)),
                barn(1.september(2008)),
                barn(1.januar(2010)),
                barn(1.januar(2012), oppholderSegIEØS = Søknad.JaNeiSpm.Nei),
                barn(1.august(2024)),
                barn(1.mars(2025)),
            )
            val resultat = barna.periodiserAntallBarn(periode)

            periode.tilDager().forEach { dag ->
                resultat.hentVerdiForDag(dag) shouldBe AntallBarn(
                    barna.count { it.oppholderSegIEØS.erJa() && it.fødselsdato <= dag && it.under16ForDato(dag) },
                )
            }
        }
    }

    @Nested
    inner class Egenskaper {
        /**
         * Sammenligner med en naiv dag-for-dag-telling for tilfeldige barn og perioder.
         * Seeden er fast, slik at en feil kan gjenskapes.
         */
        @Test
        fun `tilfeldige barn og perioder gir samme antall som en dag-for-dag-telling`() {
            val random = Random(20240701)
            val tidligst = 1.januar(2005)

            repeat(500) {
                val fraOgMed = tidligst.plusDays(random.nextLong(0, 365L * 20))
                val periode = fraOgMed til fraOgMed.plusDays(random.nextLong(0, 365L * 2))
                val barna = List(random.nextInt(0, 6)) {
                    barn(
                        fødselsdato = fraOgMed.minusYears(17).plusDays(random.nextLong(0, 365L * 20)),
                        oppholderSegIEØS = if (random.nextInt(5) == 0) Søknad.JaNeiSpm.Nei else Søknad.JaNeiSpm.Ja,
                    )
                }

                val resultat = barna.periodiserAntallBarn(periode)

                resultat.totalPeriode shouldBe periode
                resultat.perioderMedVerdi.zipWithNext().forEach { (forrige, neste) ->
                    forrige.periode.tilOgMed.plusDays(1) shouldBe neste.periode.fraOgMed
                    (forrige.verdi == neste.verdi) shouldBe false
                }
                periode.tilDager().forEach { dag ->
                    resultat.hentVerdiForDag(dag) shouldBe AntallBarn(
                        barna.count { it.oppholderSegIEØS.erJa() && it.fødselsdato <= dag && it.under16ForDato(dag) },
                    )
                }
            }
        }
    }
}
