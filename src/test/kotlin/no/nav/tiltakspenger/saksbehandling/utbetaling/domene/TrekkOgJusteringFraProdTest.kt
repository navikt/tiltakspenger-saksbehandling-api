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
internal class TrekkOgJusteringFraProdTest {

    /**
     * Trekk kommer med begge fortegn, og bare det positive telles i dag.
     *
     * I prod-uttrekket hadde saken tre positive og fire negative trekkposteringer.
     * De positive gikk alltid opp i 25 kr per dag; de negative var ujevne beløp.
     * `beregnTrekk` summerer bare de positive, så `totalTrekk` blir 250 -- mens posteringene til sammen er -224.
     *
     * TODO jah: implementasjonen er feil, se #1735.
     * Testen låser dagens oppførsel og må snus når fortegnet rettes.
     * Vurder migrering: lagrede simuleringer har det gamle tallet, og en behandling som er simulert før og kontrollsimulert etter en slik endring vil se ulikheter og bli blokkert.
     */
    @Test
    fun `bare positive trekk telles, selv når summen av alle er negativ`() {
        val simulering = simuleringForDager(
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

        val dager = simulering.simuleringPerMeldeperiode.toList().flatMap { it.simuleringsdager.toList() }

        // Summen av alle trekkposteringene: -191 + 125 - 34 + 75 - 29 - 220 + 50 = -224.
        dager.sumOf { dag ->
            dag.posteringsdag.posteringer.filter { it.type == Posteringstype.TREKK }.sumOf { it.beløp }
        } shouldBe -224

        // Men dette er tallet saksbehandler får se.
        dager.sumOf { it.totalTrekk } shouldBe 250

        // Etterbetalingen på 224 spises i sin helhet av trekket, uten at noe i oppsummeringen viser det.
        dager.sumOf { it.totalEtterbetaling } shouldBe 224
    }

    /**
     * En justering som går opp i null hos oppdragssystemet, gjør det ikke lenger etter vår avrunding.
     *
     * Dette er formen fra prod: tre justeringer på 66, -41 og -25 som summerer til nøyaktig 0, fordelt på perioder på henholdsvis fem, tre og to dager.
     * Ingen av dem går opp i antall dager, så hver av dem rundes:
     *
     * - 66 over 5 dager blir 13 per dag, altså 65 -- ett for lite.
     * - -41 over 3 dager blir -14 per dag, altså -42 -- ett for mye.
     * - -25 over 2 dager blir -12 per dag, altså -24 -- ett for lite.
     *
     * Summen ender på -1 i stedet for 0.
     *
     * `harJusteringPåTversAvMeldeperioderEllerMåneder` sjekker om summen per måned innenfor meldeperioden er ulik null.
     * Den ene kronen er nok til at vernet slår ut, og iverksetting blokkeres på en justering som er perfekt balansert innenfor én meldeperiode og én måned.
     *
     * TODO jah: dette er en falsk positiv, se #1734.
     * Testen låser dagens oppførsel og må snus når fordelingen rettes.
     */
    @Test
    fun `balansert justering blokkeres av avrundingen`() {
        val simulering = simuleringForPerioder(
            Periode(6.januar(2025), 10.januar(2025)) to listOf(ytelse(1490), justering(66)),
            Periode(13.januar(2025), 15.januar(2025)) to listOf(ytelse(894), justering(-41)),
            Periode(16.januar(2025), 17.januar(2025)) to listOf(ytelse(596), justering(-25)),
        )

        simulering.simuleringPerMeldeperiode.size shouldBe 1
        // Kilden summerer til 66 - 41 - 25 = 0, men avrundingen per dag gir 65 - 42 - 24 = -1.
        simulering.totalJustering shouldBe -1

        simulering.validerKanIverksetteUtbetaling()
            .leftOrNull() shouldBe KanIkkeIverksetteUtbetaling.JusteringStøttesIkke
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
     * Trekk går sjelden opp i antall dager -- 58 % av flerdagers trekk i prod gjør det ikke.
     *
     * TODO jah: fordelingen er en tilnærming for trekk, se #1734.
     */
    @Test
    fun `trekk fordelt over flere dager rundes av per dag`() {
        val dager = simulerPeriode(
            Periode(6.januar(2025), 10.januar(2025)),
            ytelse(1490),
            trekk(-191, Klassekoder.TREKK_KREDITOR),
        )

        dager.size shouldBe 5
        // -191 / 5 = -38,2 som rundes til -38 per dag, altså -190 til sammen.
        dager.sumOf { dag ->
            dag.posteringsdag.posteringer.filter { it.type == Posteringstype.TREKK }.sumOf { it.beløp }
        } shouldBe -190
    }
}
