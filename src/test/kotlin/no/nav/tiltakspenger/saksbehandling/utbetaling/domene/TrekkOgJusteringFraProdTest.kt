package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import org.junit.jupiter.api.Test

/**
 * Caser hentet fra et anonymisert prod-uttrekk 2026-07-27.
 *
 * Beløpene og strukturen er ekte; datoene er flyttet inn i meldeperiodene testdataene bruker.
 * Dette er de kombinasjonene vi ikke klarte å konstruere selv, fordi de oppstår av forhold hos oppdragssystemet og ikke av vår egen beregning.
 */
class TrekkOgJusteringFraProdTest {

    /**
     * Trekk kommer med begge fortegn, og alle skal telle.
     *
     * I prod-uttrekket hadde saken tre positive og fire negative trekkposteringer.
     * De positive gikk alltid opp i 25 kr per dag; de negative var ujevne beløp.
     * Tidligere summerte `beregnTrekk` bare de positive, så `totalTrekk` ble 250 -- mens posteringene til sammen er -224.
     *
     * Nå summerer vi begge fortegn, og saksbehandler ser at etterbetalingen på 224 spises i sin helhet av trekket.
     * Se #1735.
     */
    @Test
    fun `alle trekk telles, uansett fortegn`() {
        val posteringer = listOf(
            6.januar(2025) to listOf(trekk(-191, Klassekoder.TREKK_KREDITOR), trekk(125, Klassekoder.TREKK_KREDITOR)),
            13.januar(2025) to listOf(trekk(-34, Klassekoder.TREKK_KREDITOR), trekk(75, Klassekoder.TREKK_KREDITOR)),
            16.januar(2025) to listOf(
                ytelse(336),
                ytelse(-112),
                trekk(-29, Klassekoder.TREKK_KREDITOR),
                trekk(-220, Klassekoder.TREKK_BIDRAG),
                trekk(50, Klassekoder.TREKK_KREDITOR),
            ),
        )
        val simulering = simuleringForDager(*posteringer.toTypedArray())

        val dager = simulering.simuleringPerMeldeperiode.toList().flatMap { it.simuleringsdager.toList() }

        // Summen av alle trekkposteringene: -191 + 125 - 34 + 75 - 29 - 220 + 50 = -224.
        simulering.simuleringPerMeldeperiode.toList()
            .flatMap { meldeperiode -> meldeperiode.posteringer.filter { it.type == Posteringstype.TREKK } }
            .sumOf { it.beløp } shouldBe -224

        // Og det er nå tallet saksbehandler får se.
        dager.sumOf { it.totalTrekk } shouldBe -224

        dager.sumOf { it.totalEtterbetaling } shouldBe 224
    }

    /**
     * En justering som går opp i null hos oppdragssystemet, skal gjøre det hos oss også.
     *
     * Dette er formen fra prod: tre justeringer på 66, -41 og -25 som summerer til nøyaktig 0, fordelt på perioder på henholdsvis fem, tre og to dager.
     * Ingen av dem går opp i antall dager.
     *
     * Tidligere delte vi beløpet på antall dager og rundet av per dag, og da forsvant resten:
     *
     * - 66 over 5 dager ble 13 per dag, altså 65 -- ett for lite.
     * - -41 over 3 dager ble -14 per dag, altså -42 -- ett for mye.
     * - -25 over 2 dager ble -12 per dag, altså -24 -- ett for lite.
     *
     * Summen endte på -1, og `harJusteringPåTversAvMeldeperioderEllerMåneder` blokkerte en iverksetting som var perfekt balansert innenfor én meldeperiode og én måned.
     *
     * Nå summerer vernet posteringene i stedet for dagsverdiene, og den falske positiven er borte.
     * Se #1734.
     */
    @Test
    fun `balansert justering blokkeres ikke lenger`() {
        val simulering = simuleringForPerioder(
            Periode(6.januar(2025), 10.januar(2025)) to listOf(ytelse(1490), justering(66)),
            Periode(13.januar(2025), 15.januar(2025)) to listOf(ytelse(894), justering(-41)),
            Periode(16.januar(2025), 17.januar(2025)) to listOf(ytelse(596), justering(-25)),
        )

        simulering.simuleringPerMeldeperiode.size shouldBe 1
        // Kilden summerer til 66 - 41 - 25 = 0, og det gjør dagsverdiene våre nå også.
        simulering.totalJustering shouldBe 0

        simulering.validerKanIverksetteUtbetaling().isRight() shouldBe true
    }

    /** Samme justeringer, men lagt på perioder som går opp -- da blir summen null og alt går bra. */
    @Test
    fun `justering som går opp i antall dager balanserer riktig`() {
        val simulering = simuleringForPerioder(
            // 12 kr per dag i alle tre periodene: 60 - 36 - 24 = 0, uten avrunding.
            Periode(6.januar(2025), 10.januar(2025)) to listOf(ytelse(1490), justering(60)),
            Periode(13.januar(2025), 15.januar(2025)) to listOf(ytelse(894), justering(-36)),
            Periode(16.januar(2025), 17.januar(2025)) to listOf(ytelse(596), justering(-24)),
        )

        simulering.totalJustering shouldBe 0
        simulering.validerKanIverksetteUtbetaling().isRight() shouldBe true
    }

    /**
     * Trekk går sjelden opp i antall dager -- 46 % av flerdagers trekk i prod gjør det ikke.
     *
     * Posteringen beholder derfor sin egen periode og sitt eget beløp.
     * Dagsverdiene er utledet til visning, og fordelingen legger resten ut slik at summen av dagene er nøyaktig lik posteringen.
     * Se #1734.
     */
    @Test
    fun `trekk fordelt over flere dager mister ikke resten`() {
        val periode = Periode(6.januar(2025), 10.januar(2025))
        val dager = simulerPeriode(periode, ytelse(1490), trekk(-191, Klassekoder.TREKK_KREDITOR))

        dager.size shouldBe 5
        // Posteringen er bevart slik oppdragssystemet ga den.
        posteringerForPeriode(periode, ytelse(1490), trekk(-191, Klassekoder.TREKK_KREDITOR))
            .single { it.type == Posteringstype.TREKK }.beløp shouldBe -191

        // -191 / 5 gir -39, -38, -38, -38, -38, som summerer nøyaktig til -191.
        dager.sumOf { it.totalTrekk } shouldBe -191
        dager.map { it.totalTrekk } shouldBe listOf(-39, -38, -38, -38, -38)
    }
}
