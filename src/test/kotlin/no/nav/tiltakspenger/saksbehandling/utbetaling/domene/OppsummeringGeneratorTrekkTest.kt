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
 * Det samme bildet gjelder i prod: 3 214 av 3 482 trekkposteringer er negative.
 * `beregnTrekk` summerer derfor uansett fortegn, slik at feltet viser beløpet som faktisk trekkes.
 */
internal class OppsummeringGeneratorTrekkTest {

    /**
     * Slik et ekte trekk ser ut, og det skal telle med i `totalTrekk`.
     *
     * Dette er formen fra helveds `sim-trekk.xml`, med negativt beløp.
     * Tidligere summerte vi bare positive posteringer, og da ble dette trekket usynlig for saksbehandler.
     * Se #1735.
     */
    @Test
    fun `negativt trekk fra OS telles med`() {
        val dag = simulerDag(ytelse(700), trekk(-112, klassekode = Klassekoder.TREKK_BIDRAG))

        dag.totalTrekk shouldBe -112
        dag.harTrekk shouldBe true
        posteringerForDag(ytelse(700), trekk(-112, klassekode = Klassekoder.TREKK_BIDRAG)).size shouldBe 2
    }

    @Test
    fun `positivt trekk summeres`() {
        val dag = simulerDag(ytelse(408), trekk(150))

        dag.totalTrekk shouldBe 150
        dag.harTrekk shouldBe true
    }

    @Test
    fun `flere positive trekk på samme dag summeres`() {
        val dag = simulerDag(ytelse(408), trekk(100), trekk(50, klassekode = Klassekoder.TREKK_KREDITOR))

        dag.totalTrekk shouldBe 150
    }

    /**
     * Blandede fortegn summeres netto.
     *
     * De positive trekkposteringene ser ut til å være reverseringer av tidligere trekk.
     * Nettosummen er da det som faktisk trekkes fra utbetalingen.
     */
    @Test
    fun `negativt trekk trekkes fra det positive`() {
        val dag = simulerDag(ytelse(408), trekk(100), trekk(-40))

        dag.totalTrekk shouldBe 60
    }

    /** Går trekk og reversering nøyaktig opp mot hverandre, er det ikke noe trekk igjen å vise. */
    @Test
    fun `trekk som nulles ut av en reversering gir null`() {
        val dag = simulerDag(ytelse(408), trekk(100), trekk(-100))

        dag.totalTrekk shouldBe 0
        dag.harTrekk shouldBe false
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
        posteringerForDag(ytelse(408), forskudsskatt(-100)).size shouldBe 2
    }
}
