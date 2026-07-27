package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Klassekodenes betydning.
 *
 * Merk hva dette handler om: ytelsesposteringene er selve utbetalingen, og de driver `nyUtbetaling`, `tidligereUtbetalt` og `totalEtterbetaling` på nesten hver eneste dag.
 * Det som ikke betyr noe, er hvilken av de 20 ytelseskodene som brukes -- de behandles helt likt.
 *
 * Dev-uttrekket inneholdt 23 klassekoder, og bare to av dem endrer utregningen ut fra koden alene:
 * `KL_KODE_FEIL_ARBYT` og `KL_KODE_JUST_ARBYT`.
 * Testene her låser det skillet, slik at en ny klassekode ikke stille endrer oppførselen.
 */
internal class OppsummeringGeneratorKlassekodeTest {

    @Test
    fun `alle observerte ytelsesklassekoder behandles likt`() {
        Klassekoder.YTELSE_TILTAKSPENGER.forEach { klassekode ->
            withClue(klassekode) {
                val dag = simulerDag(ytelse(408, klassekode = klassekode))
                dag.nyUtbetaling shouldBe 408
                dag.totalEtterbetaling shouldBe 408
            }
        }
    }

    @Test
    fun `barnetillegg behandles som en hvilken som helst annen ytelse`() {
        Klassekoder.YTELSE_BARNETILLEGG.forEach { klassekode ->
            withClue(klassekode) {
                val dag = simulerDag(ytelse(110, klassekode = klassekode))
                dag.nyUtbetaling shouldBe 110
            }
        }
    }

    @Test
    fun `tiltakspenger og barnetillegg summeres på samme dag`() {
        val dag = simulerDag(
            ytelse(298, klassekode = "TPTPOPPFAG"),
            ytelse(110, klassekode = "TPBTOPPFAGR"),
        )

        dag.nyUtbetaling shouldBe 408
    }

    @Test
    fun `bare feilutbetalingsklassekoden gir feilutbetaling`() {
        val medRiktigKode = simulerDag(ytelse(400), feilutbetaling(100))
        val medFeilKode = simulerDag(
            ytelse(400),
            Testpostering(type = "FEILUTBETALING", klassekode = "EN_ANNEN_KODE", beløp = 100),
        )

        medRiktigKode.totalFeilutbetaling shouldBe 100
        medRiktigKode.nyUtbetaling shouldBe 300

        medFeilKode.totalFeilutbetaling shouldBe 0
        medFeilKode.nyUtbetaling shouldBe 400
    }

    @Test
    fun `bare justeringsklassekoden gir justering`() {
        val medRiktigKode = simulerDag(ytelse(400), justering(-100))
        val medFeilKode = simulerDag(
            ytelse(400),
            Testpostering(type = "JUSTERING", klassekode = "EN_ANNEN_KODE", beløp = -100),
        )

        medRiktigKode.totalJustering shouldBe -100
        medRiktigKode.harJustering shouldBe true

        medFeilKode.totalJustering shouldBe 0
        medFeilKode.harJustering shouldBe false
    }

    @Test
    fun `motposteringer telles uansett klassekode`() {
        val dag = simulerDag(
            ytelse(400),
            Testpostering(type = "MOTPOSTERING", klassekode = "EN_ANNEN_KODE", beløp = -100),
        )

        dag.totalMotpostering shouldBe -100
    }

    @Test
    fun `klassekoden bevares i posteringene`() {
        posteringerForDag(ytelse(408, klassekode = "TPTPGRVGSHOY")).single().klassekode shouldBe "TPTPGRVGSHOY"
    }

    /**
     * Fester antallet til det vi faktisk observerte, slik at listen må oppdateres bevisst.
     *
     * Dukker det opp en klassekode nummer 24 i et nytt uttrekk, er det verdt å vurdere om den skal behandles som noe annet enn ren informasjon.
     */
    @Test
    fun `alle observerte klassekoder er dokumentert`() {
        Klassekoder.ALLE_OBSERVERTE.size shouldBe 23
        Klassekoder.ALLE_OBSERVERTE.distinct().size shouldBe 23
        Klassekoder.ALLE_OBSERVERTE.contains(OppsummeringGenerator.KLASSEKODE_FEILUTBETALING) shouldBe true
        Klassekoder.ALLE_OBSERVERTE.contains(OppsummeringGenerator.KLASSEKODE_JUSTERING) shouldBe true
    }
}
