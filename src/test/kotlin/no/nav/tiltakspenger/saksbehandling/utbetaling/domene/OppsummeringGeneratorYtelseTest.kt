package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import org.junit.jupiter.api.Test

/**
 * Rene ytelsesposteringer.
 *
 * Dette er den klart vanligste formen: 4946 av dagene i dev-uttrekket besto bare av positive ytelser.
 */
class OppsummeringGeneratorYtelseTest {

    @Test
    fun `bare positive ytelser gir ny utbetaling og etterbetaling`() {
        val dag = simulerDag(ytelse(298), ytelse(110))

        dag.tidligereUtbetalt shouldBe 0
        dag.nyUtbetaling shouldBe 408
        dag.totalEtterbetaling shouldBe 408
        dag.totalFeilutbetaling shouldBe 0
        dag.totalMotpostering shouldBe 0
        dag.totalTrekk shouldBe 0
        dag.totalJustering shouldBe 0
        dag.harJustering shouldBe false
    }

    @Test
    fun `negative ytelser blir til tidligere utbetalt`() {
        val dag = simulerDag(ytelse(-312), ytelse(-112))

        dag.tidligereUtbetalt shouldBe 424
        dag.nyUtbetaling shouldBe 0
        dag.totalEtterbetaling shouldBe 0
    }

    @Test
    fun `uendret beløp gir ingen etterbetaling`() {
        val dag = simulerDag(ytelse(312), ytelse(112), ytelse(-312), ytelse(-112))

        dag.tidligereUtbetalt shouldBe 424
        dag.nyUtbetaling shouldBe 424
        dag.totalEtterbetaling shouldBe 0
    }

    @Test
    fun `økning gir etterbetaling av differansen`() {
        val dag = simulerDag(ytelse(168), ytelse(-112))

        dag.tidligereUtbetalt shouldBe 112
        dag.nyUtbetaling shouldBe 168
        dag.totalEtterbetaling shouldBe 56
    }

    @Test
    fun `reduksjon gir ikke negativ etterbetaling`() {
        val dag = simulerDag(ytelse(100), ytelse(-300))

        dag.tidligereUtbetalt shouldBe 300
        dag.nyUtbetaling shouldBe 100
        dag.totalEtterbetaling shouldBe 0
    }

    @Test
    fun `posteringer på andre fagområder filtreres bort`() {
        val dag = simulerDag(ytelse(408), annetFagområde(9999))

        dag.nyUtbetaling shouldBe 408
        val posteringer = posteringerForDag(ytelse(408), annetFagområde(9999))
        posteringer.size shouldBe 1
        posteringer.single().fagområde shouldBe FAGOMRÅDE_TILTAKSPENGER
    }

    @Test
    fun `beløp fordeles likt over dagene i perioden`() {
        val dager = simulerPeriode(Periode(6.januar(2025), 9.januar(2025)), ytelse(400))

        dager.size shouldBe 4
        dager.map { it.nyUtbetaling } shouldBe listOf(100, 100, 100, 100)
    }

    /**
     * Fordelingen mister ikke resten, så summen av dagene er alltid nøyaktig beløpet OS sendte.
     *
     * 101 kroner over to dager blir 50,5 per dag.
     * Tidligere rundet vi hver dag for seg og fikk 51 + 51 = 102, som var kilden til avvikene på én og to kroner mellom `simulering` og `simulering_metadata` i dev-uttrekket.
     * Nå legges resten på den første dagen i stedet, og summen går opp.
     * Se #1734.
     */
    @Test
    fun `fordelingen per dag summerer til beløpet fra OS`() {
        val dager = simulerPeriode(Periode(6.januar(2025), 7.januar(2025)), ytelse(101))

        dager.map { it.nyUtbetaling } shouldBe listOf(51, 50)
        dager.sumOf { it.nyUtbetaling } shouldBe 101
    }

    @Test
    fun `simuleringen får med seg datoBeregnet og totalBeløp`() {
        val simulering = simulering(Periode(6.januar(2025), 6.januar(2025)), ytelse(408))

        simulering.datoBeregnet shouldBe 6.januar(2025)
        simulering.totalBeløp shouldBe 408
    }
}
