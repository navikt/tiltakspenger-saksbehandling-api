package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Trekk og forskuddsskatt.
 *
 * Ingen av delene forekom i dev-uttrekket fra 2026-07-27: null TREKK-posteringer på 472 simuleringer, og null FORSKUDSSKATT.
 * Beløp, fortegn og klassekoder her er derfor hentet fra ekte OS-responser i andre repoer:
 *
 * - helved, `apps/simulering/test/sim-trekk.xml`: `belop=-112.00`, `typeKlasse=TREK`,
 * `klassekode=BSKTKRED`
 * - su-se-bakover, `simulering-dobbel-tilbakeføring-med-trekk.xml`: månedlige trekk på
 * `-2300.00`, `klassekode=AVSUINTE`
 * - su-se-bakover, `simulering-trek-deler-opp-en-måned-i-to.xml`: trekk på `-20285.00` per måned
 *
 * **I alle 14 trekkposteringene jeg har funnet er beløpet negativt.**
 * Det står i motstrid til kommentaren på `beregnTrekk` ("Kommer som positive posteringer") og til implementasjonen, som bare summerer de positive.
 * Testene under låser dagens oppførsel og viser konsekvensen.
 */
internal class OppsummeringGeneratorTrekkTest {

    /**
     * Slik et ekte trekk ser ut -- og det havner ikke i `totalTrekk`.
     *
     * Dette er formen fra helveds `sim-trekk.xml`, med negativt beløp.
     * `beregnTrekk` summerer bare positive posteringer, så resultatet blir 0 og `harTrekk` false.
     * Testen påstår ikke at dette er riktig; den dokumenterer at vi i dag ikke ville fanget opp et trekk fra OS i det hele tatt.
     */
    @Test
    fun `negativt trekk fra OS gir null totalTrekk`() {
        val dag = simulerDag(ytelse(700), trekk(-112, klassekode = Klassekoder.TREKK_BIDRAG))

        dag.totalTrekk shouldBe 0
        dag.harTrekk shouldBe false
        // Posteringen er bevart, så informasjonen er ikke tapt -- den er bare ikke summert.
        dag.posteringsdag.posteringer.size shouldBe 2
    }

    @Test
    fun `positivt trekk summeres`() {
        val dag = simulerDag(ytelse(408), trekk(150))

        dag.totalTrekk shouldBe 150
        dag.harTrekk shouldBe true
    }

    @Test
    fun `flere positive trekk på samme dag summeres`() {
        val dag = simulerDag(ytelse(408), trekk(100), trekk(50, klassekode = Klassekoder.TREKK_AVDRAG))

        dag.totalTrekk shouldBe 150
    }

    /** Blandede fortegn: bare den positive delen teller. */
    @Test
    fun `negativt trekk trekkes ikke fra det positive`() {
        val dag = simulerDag(ytelse(408), trekk(100), trekk(-40))

        dag.totalTrekk shouldBe 100
    }

    /**
     * Trekket er informasjon ved siden av utbetalingen, ikke en del av den.
     *
     * Det reduserer altså verken ny utbetaling eller etterbetaling, uansett fortegn.
     */
    @Test
    fun `trekk påvirker ikke utbetalingen`() {
        val utenTrekk = simulerDag(ytelse(700))
        val medPositivtTrekk = simulerDag(ytelse(700), trekk(112))
        val medNegativtTrekk = simulerDag(ytelse(700), trekk(-112))

        listOf(medPositivtTrekk, medNegativtTrekk).forEach {
            it.nyUtbetaling shouldBe utenTrekk.nyUtbetaling
            it.totalEtterbetaling shouldBe utenTrekk.totalEtterbetaling
            it.tidligereUtbetalt shouldBe utenTrekk.tidligereUtbetalt
        }
    }

    @Test
    fun `trekk kombinert med feilutbetaling`() {
        val dag = simulerDag(
            ytelse(312),
            ytelse(-312),
            feilutbetaling(200),
            motpostering(-200),
            trekk(50),
        )

        dag.totalTrekk shouldBe 50
        dag.totalFeilutbetaling shouldBe 200
        dag.totalMotpostering shouldBe -200
        dag.nyUtbetaling shouldBe 112
        dag.tidligereUtbetalt shouldBe 312
    }

    /** Klassekoden er uten betydning for trekk -- det er bare posteringstypen som teller. */
    @Test
    fun `trekk telles uansett klassekode`() {
        val dag = simulerDag(ytelse(408), trekk(150, klassekode = "EN_HELT_ANNEN_KODE"))

        dag.totalTrekk shouldBe 150
    }

    /**
     * FORSKUDSSKATT er med i posteringstypene, men ingen av utregningene ser på den.
     *
     * Formen er fra su-se-bakover, der forskuddsskatt kommer som `typeKlasse=SKAT`, `klassekode=FSKTSKAT` med negativt beløp.
     * helved-dokumentasjonen nevner at dagpenger og AAP vil få slike posteringer.
     */
    @Test
    fun `forskuddsskatt påvirker ingen sum`() {
        val dag = simulerDag(ytelse(408), forskudsskatt(-100))

        dag.nyUtbetaling shouldBe 408
        dag.tidligereUtbetalt shouldBe 0
        dag.totalEtterbetaling shouldBe 408
        dag.totalTrekk shouldBe 0
        dag.totalFeilutbetaling shouldBe 0
        dag.totalMotpostering shouldBe 0
        dag.posteringsdag.posteringer.size shouldBe 2
    }
}
