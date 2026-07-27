package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Justeringer.
 *
 * Justeringer har skiftet form underveis, og dev-uttrekket inneholder begge variantene:
 *
 * - Fram til mars 2026 kom de som `FEILUTBETALING` med klassekoden `KL_KODE_JUST_ARBYT`.
 * - Deretter begynte helved å sende dem som en egen posteringstype, `JUSTERING`.
 *
 * Vi fulgte etter først 20. april 2026, i commit 68f3a74aa.
 * I mellomrommet summerte vi fortsatt på `FEILUTBETALING`, så justeringene ble stilltiende nullet:
 * uttrekket har dager med en `JUSTERING`-postering der lagret `totalJustering` er 0 og `harJustering` er false.
 *
 * Testene her låser dagens oppførsel og dokumenterer hva den gamle ga, slik at skiftet ikke blir borte neste gang noen leser tallene.
 */
internal class OppsummeringGeneratorJusteringTest {

    /**
     * Begge formene skal telle som justering.
     *
     * Klassekoden har ligget fast gjennom hele materialet vi har sett; posteringstypen har opptrådt som både FEILUTBETALING og JUSTERING for samme begrep -- på tvers av ytelser og tid.
     * Supplerende Stønad møtte `JUST` allerede i juli 2025, i kravgrunnlag.
     * Vi vet ikke hva OS' målbilde er, så vi må tåle at de veksler.
     */
    @Test
    fun `justering gjenkjennes på klassekoden, uansett posteringstype`() {
        val somJustering = simulerDag(ytelse(212), ytelse(106), justering(-106))
        val somFeilutbetaling = simulerDag(
            ytelse(212),
            ytelse(106),
            feilutbetalingMedJusteringsklassekode(-106),
        )

        somJustering.totalJustering shouldBe -106
        somJustering.harJustering shouldBe true

        somFeilutbetaling.totalJustering shouldBe -106
        somFeilutbetaling.harJustering shouldBe true

        // Formen skal ikke gi ulikt resultat.
        somFeilutbetaling.totalEtterbetaling shouldBe somJustering.totalEtterbetaling
        somFeilutbetaling.nyUtbetaling shouldBe somJustering.nyUtbetaling
    }

    /** Justeringsklassekoden skal aldri telle som feilutbetaling, uansett posteringstype. */
    @Test
    fun `justeringsklassekoden gir ikke feilutbetaling`() {
        val dag = simulerDag(
            ytelse(400),
            feilutbetalingMedJusteringsklassekode(100),
            feilutbetaling(50),
        )

        dag.totalFeilutbetaling shouldBe 50
        dag.totalJustering shouldBe 100
        // Bare den ekte feilutbetalingen trekkes fra ny utbetaling.
        dag.nyUtbetaling shouldBe 350
    }

    @Test
    fun `negativ justering reduserer etterbetalingen`() {
        val dag = simulerDag(ytelse(212), ytelse(106), justering(-106))

        dag.nyUtbetaling shouldBe 318
        dag.tidligereUtbetalt shouldBe 0
        dag.totalJustering shouldBe -106
        dag.harJustering shouldBe true
        // 318 - 0 - |−106|
        dag.totalEtterbetaling shouldBe 212
    }

    @Test
    fun `positiv justering reduserer ikke etterbetalingen`() {
        val dag = simulerDag(
            ytelse(212),
            ytelse(106),
            ytelse(-312),
            ytelse(-112),
            justering(106),
        )

        dag.nyUtbetaling shouldBe 318
        dag.tidligereUtbetalt shouldBe 424
        dag.totalJustering shouldBe 106
        dag.harJustering shouldBe true
        dag.totalEtterbetaling shouldBe 0
    }

    @Test
    fun `justering påvirker verken feilutbetaling eller motpostering`() {
        val dag = simulerDag(ytelse(400), justering(-100))

        dag.totalFeilutbetaling shouldBe 0
        dag.totalMotpostering shouldBe 0
        dag.totalJustering shouldBe -100
    }

    /**
     * `harJustering` ser etter om posteringen finnes, ikke om summen er ulik null.
     *
     * Merk at defaultverdien i databaselaget utleder den som `totalJustering < 0`, jf.
     * `SimuleringEndringDbJson.Simuleringsdag`.
     * De to reglene er ikke like: en dag med to justeringer som nuller hverandre ut får `harJustering = true` her, men ville fått false av defaultverdien.
     */
    @Test
    fun `harJustering er sann selv når justeringene nuller hverandre ut`() {
        val dag = simulerDag(ytelse(400), justering(100), justering(-100))

        dag.totalJustering shouldBe 0
        dag.harJustering shouldBe true
    }

    /**
     * Den gamle formen teller igjen.
     *
     * Denne dagen har nøyaktig formen til 13 dager i dev-uttrekket, som den gang ble lagret med `totalJustering = -55` og `harJustering = true`.
     * Mellom commit 68f3a74aa og i dag ga de samme posteringene 0 og false, fordi vi filtrerte på posteringstypen.
     * Nå gjenkjenner vi dem igjen, uten å miste den nye formen.
     */
    @Test
    fun `feilutbetaling med justeringsklassekode teller som justering`() {
        val dag = simulerDag(
            ytelse(165),
            ytelse(-110),
            feilutbetalingMedJusteringsklassekode(-55),
        )

        dag.totalJustering shouldBe -55
        dag.harJustering shouldBe true
        // 165 - 110 = 55, minus justeringen på 55.
        dag.totalEtterbetaling shouldBe 0
    }

    /**
     * Justeringsfradraget forsvinner når dagen ikke har etterbetaling å trekke fra.
     *
     * helved regner oppsummeringen sin **per måned**, og dokumentasjonen bygger på det:
     * en positiv justering én dag og en negativ en annen dag kansellerer hverandre i månedssummen.
     * Vi regner **per dag**, og gulver på 0 for hver dag.
     * Lander den negative justeringen på en dag uten etterbetaling, blir fradraget borte i stedet for å redusere de andre dagene.
     *
     * I dev-uttrekket summerte alle justeringssett til nøyaktig 0 på tvers av responsen, og to av radene har dagsummer som avviker fra helveds månedsoppsummering med akkurat justeringsbeløpet.
     *
     * Testen låser dagens oppførsel.
     * Om den er ønsket er et åpent spørsmål.
     */
    @Test
    fun `negativ justering på en dag uten etterbetaling gir ingen reduksjon`() {
        val dagMedJustering = simulerDag(ytelse(100), ytelse(-100), justering(-1272))

        dagMedJustering.nyUtbetaling shouldBe 100
        dagMedJustering.tidligereUtbetalt shouldBe 100
        dagMedJustering.totalJustering shouldBe -1272
        // maxOf(100 - 100 - 1272, 0) -- fradraget på 1272 forsvinner i gulvet.
        dagMedJustering.totalEtterbetaling shouldBe 0
    }

    /**
     * En FEILUTBETALING med justeringsklassekoden slår ut på justeringen, men ikke på noe annet.
     *
     * Den teller ikke som feilutbetaling, fordi `beregnFeilutbetaling` krever feilutbetalingsklassekoden, og den reduserer heller ikke ny utbetaling.
     * En positiv justering reduserer ikke etterbetalingen.
     */
    @Test
    fun `feilutbetaling med justeringsklassekode slår bare ut på justeringen`() {
        val utenJustering = simulerDag(ytelse(400))
        val medJustering = simulerDag(ytelse(400), feilutbetalingMedJusteringsklassekode(200))

        medJustering.nyUtbetaling shouldBe utenJustering.nyUtbetaling
        medJustering.totalFeilutbetaling shouldBe utenJustering.totalFeilutbetaling
        medJustering.totalEtterbetaling shouldBe utenJustering.totalEtterbetaling

        medJustering.totalJustering shouldBe 200
        medJustering.harJustering shouldBe true
        utenJustering.totalJustering shouldBe 0
        medJustering.posteringsdag.posteringer.size shouldBe 2
    }
}
