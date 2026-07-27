package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import org.junit.jupiter.api.Test

/**
 * Sammensatte dager, hentet fra dev-uttrekket 2026-07-27.
 *
 * De enkeltvise reglene er dekket av de andre testfilene.
 * Her er poenget kombinasjonene: feilutbetaling, motpostering og justering på samme dag, som er der utregningene griper inn i hverandre.
 * Hver test viser hvor mange dager i uttrekket som hadde den formen.
 */
internal class OppsummeringGeneratorKombinasjonerTest {

    /**
     * 13 dager i uttrekket.
     *
     * Dagen har både en ekte feilutbetaling og en justering, der justeringen kommer som FEILUTBETALING med justeringsklassekoden.
     * De to må holdes fra hverandre: bare feilutbetalingsklassekoden gir feilutbetaling og reduserer ny utbetaling, mens justeringsklassekoden gir justering.
     */
    @Test
    fun `feilutbetaling og justering med samme posteringstype på samme dag`() {
        val dag = simulerDag(
            ytelse(212),
            ytelse(106),
            ytelse(-212),
            ytelse(-106),
            feilutbetaling(265),
            feilutbetalingMedJusteringsklassekode(53),
            motpostering(-265),
        )

        dag.tidligereUtbetalt shouldBe 318
        dag.totalFeilutbetaling shouldBe 265
        dag.totalMotpostering shouldBe -265
        dag.nyUtbetaling shouldBe 53
        dag.totalEtterbetaling shouldBe 0
        dag.totalJustering shouldBe 53
        dag.harJustering shouldBe true
    }

    /**
     * 7 dager i uttrekket.
     *
     * Negativ feilutbetaling, positiv motpostering og en justering -- alt på en dag der det bare er negative ytelser.
     */
    @Test
    fun `tilbakeført feilutbetaling med justering`() {
        val dag = simulerDag(
            ytelse(-212),
            ytelse(-156),
            feilutbetaling(-368),
            justering(368),
            motpostering(368),
        )

        dag.tidligereUtbetalt shouldBe 368
        dag.nyUtbetaling shouldBe 0
        dag.totalFeilutbetaling shouldBe 0
        dag.totalMotpostering shouldBe 368
        dag.totalJustering shouldBe 368
        dag.harJustering shouldBe true
        dag.totalEtterbetaling shouldBe 0
    }

    /**
     * 2 dager i uttrekket, og den eneste observerte formen som gir negativ ny utbetaling.
     *
     * Feilutbetalingen er større enn den positive ytelsen på dagen.
     */
    @Test
    fun `feilutbetaling større enn ytelsen gir negativ ny utbetaling`() {
        val dag = simulerDag(
            ytelse(37),
            ytelse(-212),
            ytelse(-196),
            feilutbetaling(51),
            motpostering(-50),
        )

        dag.tidligereUtbetalt shouldBe 408
        dag.nyUtbetaling shouldBe -14
        dag.totalFeilutbetaling shouldBe 51
        dag.totalMotpostering shouldBe -50
        dag.totalEtterbetaling shouldBe 0
    }

    /**
     * Motposteringen er ikke alltid nøyaktig den negative feilutbetalingen.
     *
     * Kontrakten sier at `totalMotpostering` skal være lik negativ `totalFeilutbetaling`, men i uttrekket fant vi dager der de skiller seg med én krone, som følge av avrunding per dag.
     */
    @Test
    fun `motpostering trenger ikke være nøyaktig negativ feilutbetaling`() {
        val dag = simulerDag(
            ytelse(-408),
            feilutbetaling(408),
            motpostering(-407),
        )

        dag.totalFeilutbetaling shouldBe 408
        dag.totalMotpostering shouldBe -407
    }

    /** En dag uten posteringer i det hele tatt finnes ikke -- simuleringen blir da IngenEndring. */
    @Test
    fun `simulering uten posteringer gir ingen endring`() {
        val simulering = simuleringResultat(Periode(6.januar(2025), 6.januar(2025)))

        (simulering is Simulering.IngenEndring) shouldBe true
    }

    /** Dager utenfor meldeperioden faller ut av oppsummeringen. */
    @Test
    fun `bare dager som ligger i en meldeperiode kommer med`() {
        val dager = simulerPeriode(Periode(4.januar(2025), 7.januar(2025)), ytelse(400))

        // Meldeperioden starter mandag 6. januar, så 4. og 5. januar faller utenfor.
        dager.map { it.dato } shouldBe listOf(6.januar(2025), 7.januar(2025))
    }
}
