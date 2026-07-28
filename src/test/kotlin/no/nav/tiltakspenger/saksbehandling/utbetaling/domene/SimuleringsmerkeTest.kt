package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import org.junit.jupiter.api.Test

/**
 * Merket sier hvilke dager en postering dekker, ikke hvor mye av den som hører til hver dag.
 *
 * Perioden er kildedata og kan alltid vises.
 * Beløpet kan bare vises når posteringen dekker én dag -- ellers finnes det ingen dagsandel å oppgi.
 */
internal class SimuleringsmerkeTest {

    @Test
    fun `endagspostering beholder beløpet`() {
        val merke = posteringerForDag(ytelse(408)).single().tilSimuleringsmerke()

        merke.beløp shouldBe 408
        merke.periode shouldBe Periode(Simuleringstestdata.førsteDag, Simuleringstestdata.førsteDag)
        merke.type shouldBe Posteringstype.YTELSE
    }

    /**
     * Fem dager med 1490 kroner går opp i 298 per dag, men det er ikke poenget.
     * Vi oppgir ikke en dagsandel selv når den tilfeldigvis går opp, fordi kilden ikke har sagt at den fordeler seg slik.
     */
    @Test
    fun `flerdagerspostering har ingen dagsandel å oppgi`() {
        val periode = Periode(6.januar(2025), 10.januar(2025))
        val merke = posteringerForPeriode(periode, ytelse(1490)).single().tilSimuleringsmerke()

        merke.beløp shouldBe null
        merke.periode shouldBe periode
    }

    @Test
    fun `justering gjenkjennes på klassekoden, også i merket`() {
        val merker = posteringerForDag(ytelse(400), justering(-50)).map { it.tilSimuleringsmerke() }

        merker.single { it.erJustering }.beløp shouldBe -50
        merker.count { it.erJustering } shouldBe 1
    }

    @Test
    fun `fortegnet er kildedata og følger med selv når beløpet ikke kan vises`() {
        val periode = Periode(Simuleringstestdata.førsteDag, Simuleringstestdata.førsteDag.plusDays(4))
        val merker = posteringerForPeriode(periode, trekk(-237), trekk(156)).map { it.tilSimuleringsmerke() }

        merker.map { it.beløp } shouldBe listOf(null, null)
        merker.map { it.erNegativt } shouldBe listOf(true, false)
    }
}

/**
 * Flaggene er fakta om meldeperioden, ikke dommer.
 * Se [Simuleringsflagg].
 */
internal class SimuleringsflaggTest {

    /**
     * Prod-casen fra [TrekkOgJusteringFraProdTest]: 66, -41 og -25 summerer til null hos oppdragssystemet.
     * Den skal flagges som en balansert justering, ikke som noe som sperrer.
     */
    @Test
    fun `justering som balanserer innenfor måneden går opp i null`() {
        val meldeperiode = simuleringForPerioder(
            Periode(6.januar(2025), 10.januar(2025)) to listOf(ytelse(1490), justering(66)),
            Periode(13.januar(2025), 15.januar(2025)) to listOf(ytelse(894), justering(-41)),
            Periode(16.januar(2025), 17.januar(2025)) to listOf(ytelse(596), justering(-25)),
        ).simuleringPerMeldeperiode.single()

        meldeperiode.harUbalansertJustering shouldBe false

        val flagg = Simuleringsflagg.fraPosteringer(meldeperiode.posteringer, meldeperiode.harUbalansertJustering)
        flagg.harJustering shouldBe true
        flagg.justeringGårOppINull shouldBe true
        flagg.justeringPåTversAvMeldeperiodeEllerMåned shouldBe false
    }

    @Test
    fun `justering som ikke balanserer flagges som på tvers`() {
        val meldeperiode = simuleringForPerioder(
            Periode(6.januar(2025), 10.januar(2025)) to listOf(ytelse(1490), justering(66)),
        ).simuleringPerMeldeperiode.single()

        meldeperiode.harUbalansertJustering shouldBe true

        val flagg = Simuleringsflagg.fraPosteringer(meldeperiode.posteringer, meldeperiode.harUbalansertJustering)
        flagg.justeringGårOppINull shouldBe false
        flagg.justeringPåTversAvMeldeperiodeEllerMåned shouldBe true
    }

    /** Trekk er negativt i de aller fleste tilfellene, så flagget kan ikke sjekke om beløpet er positivt. */
    @Test
    fun `negativt trekk flagges`() {
        val meldeperiode = simulering(
            Periode(6.januar(2025), 10.januar(2025)),
            ytelse(1490),
            trekk(-191),
        ).simuleringPerMeldeperiode.single()

        val flagg = Simuleringsflagg.fraPosteringer(meldeperiode.posteringer, meldeperiode.harUbalansertJustering)
        flagg.harTrekk shouldBe true
        flagg.harJustering shouldBe false
        flagg.harFeilutbetaling shouldBe false
    }

    @Test
    fun `en meldeperiode uten simulering flagger ingenting`() {
        Simuleringsflagg.ingenSimulering.harJustering shouldBe false
        Simuleringsflagg.ingenSimulering.justeringGårOppINull shouldBe false
        Simuleringsflagg.ingenSimulering.harTrekk shouldBe false
    }

    @Test
    fun `feilutbetaling krever feilutbetalingsklassekoden`() {
        val medEkteFeilutbetaling = simulering(
            Periode(6.januar(2025), 6.januar(2025)),
            ytelse(312),
            ytelse(-312),
            feilutbetaling(200),
            motpostering(-200),
        ).simuleringPerMeldeperiode.single()

        val medJusteringsklassekode = simulering(
            Periode(6.januar(2025), 6.januar(2025)),
            ytelse(400),
            feilutbetalingMedJusteringsklassekode(200),
        ).simuleringPerMeldeperiode.single()

        Simuleringsflagg.fraPosteringer(
            medEkteFeilutbetaling.posteringer,
            medEkteFeilutbetaling.harUbalansertJustering,
        ).harFeilutbetaling shouldBe true

        Simuleringsflagg.fraPosteringer(
            medJusteringsklassekode.posteringer,
            medJusteringsklassekode.harUbalansertJustering,
        ).let {
            it.harFeilutbetaling shouldBe false
            it.harJustering shouldBe true
        }
    }

    /** En negativ feilutbetalingspostering er en reversering av et tidligere krav, ikke et nytt kravgrunnlag. */
    @Test
    fun `negativ feilutbetaling flagges ikke`() {
        val meldeperiode = simulering(
            Periode(6.januar(2025), 6.januar(2025)),
            ytelse(312),
            feilutbetaling(-200),
            motpostering(200),
        ).simuleringPerMeldeperiode.single()

        meldeperiode.flagg.harFeilutbetaling shouldBe false
        meldeperiode.harFeilutbetaling shouldBe false
    }
}
