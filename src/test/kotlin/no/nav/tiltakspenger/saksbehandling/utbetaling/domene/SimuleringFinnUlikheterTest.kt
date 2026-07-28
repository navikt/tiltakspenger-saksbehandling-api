package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.clock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.meldeperiode
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Kontrollsimuleringen sammenlignes med simuleringen beslutter så, på posteringene.
 *
 * Posteringene er kildedataene fra oppdragssystemet.
 * Dagsverdiene er utledet av dem til visning og kan ikke avvike uten at en postering avviker, så de sammenlignes ikke.
 */
class SimuleringFinnUlikheterTest {
    private val periode = Periode(13.januar(2025), 26.januar(2025))
    private val meldeperiode = meldeperiode(periode = periode)

    private fun postering(
        fraOgMed: LocalDate = periode.fraOgMed,
        tilOgMed: LocalDate = fraOgMed,
        beløp: Int = 255,
    ): Postering {
        return Postering(
            periode = Periode(fraOgMed, tilOgMed),
            fagområde = "TILTAKSPENGER",
            beløp = beløp,
            type = Posteringstype.YTELSE,
            klassekode = "test_klassekode",
        )
    }

    private fun simulering(
        meldeperiode: Meldeperiode = this.meldeperiode,
        tidligereUtbetalt: Int = 0,
        nyUtbetaling: Int = 255,
        posteringer: NonEmptyList<Postering> = nonEmptyListOf(postering(beløp = nyUtbetaling)),
    ): Simulering.Endring {
        val dato = periode.fraOgMed
        return Simulering.Endring(
            datoBeregnet = dato,
            totalBeløp = nyUtbetaling,
            simuleringPerMeldeperiode = nonEmptyListOf(
                SimuleringForMeldeperiode(
                    meldeperiode = meldeperiode,
                    simuleringsdager = nonEmptyListOf(
                        simuleringsdag(
                            dato = dato,
                            tidligereUtbetalt = tidligereUtbetalt,
                            nyUtbetaling = nyUtbetaling,
                        ),
                    ),
                    posteringer = posteringer,
                ),
            ),
            simuleringstidspunkt = nå(clock),
        )
    }

    private fun simuleringsdag(
        dato: LocalDate,
        tidligereUtbetalt: Int,
        nyUtbetaling: Int,
    ): Simuleringsdag {
        return Simuleringsdag(
            dato = dato,
            tidligereUtbetalt = tidligereUtbetalt,
            nyUtbetaling = nyUtbetaling,
            totalEtterbetaling = 0,
            totalFeilutbetaling = 0,
            totalMotpostering = 0,
            totalTrekk = 0,
            totalJustering = 0,
            harJustering = false,
        )
    }

    @Test
    fun `like simuleringer gir ingen ulikheter`() {
        simulering().finnUlikheter(simulering()) shouldBe emptyList()
        simulering().erLik(simulering()) shouldBe true
        (null as Simulering?).finnUlikheter(null) shouldBe emptyList()
    }

    /** Rekkefølgen posteringene kom i fra oppdragssystemet er ikke en del av innholdet. */
    @Test
    fun `posteringer i en annen rekkefølge er ikke en endring`() {
        val a = postering(fraOgMed = 13.januar(2025), beløp = 255)
        val b = postering(fraOgMed = 14.januar(2025), beløp = -255)

        val beregnet = simulering(posteringer = nonEmptyListOf(a, b))
        val kontroll = simulering(posteringer = nonEmptyListOf(b, a))

        beregnet.finnUlikheter(kontroll) shouldBe emptyList()
    }

    @Test
    fun `manglende simulering mot endring beskriver begge sider`() {
        val kontroll = simulering(tidligereUtbetalt = 0, nyUtbetaling = 255)

        (null as Simulering?).finnUlikheter(kontroll) shouldBe listOf(
            "Ulike simuleringstyper: beregnet=mangler, kontroll=endring (totalPeriode=$periode, tidligereUtbetalt=0, nyUtbetaling=255, totalEtterbetaling=0, totalFeilutbetaling=0, totalJustering=0, totalTrekk=0)",
        )
    }

    @Test
    fun `endrede posteringer beskrives med begge sider`() {
        val beregnet = simulering(tidligereUtbetalt = 0, nyUtbetaling = 255)
        val kontroll = simulering(tidligereUtbetalt = 255, nyUtbetaling = 510)

        beregnet.finnUlikheter(kontroll) shouldBe listOf(
            "Meldeperiode ${meldeperiode.id} har ulike posteringer." +
                " Kun i beregnet: YTELSE/test_klassekode 2025-01-13–2025-01-13 255 kr (TILTAKSPENGER)." +
                " Kun i kontroll: YTELSE/test_klassekode 2025-01-13–2025-01-13 510 kr (TILTAKSPENGER).",
        )
    }

    /**
     * Eldre lagrede simuleringer har posteringene splittet opp per dag, mens en fersk kontrollsimulering har periodene fra oppdragssystemet.
     * Samme innhold i ulik form regnes som en endring, med vilje: tallene beslutter så på skal være tallene som iverksettes, og saksbehandler løser det med «Oppdater simulering».
     */
    @Test
    fun `dagsplittede posteringer fra en eldre lagret simulering er en endring mot dagens form`() {
        val gammelForm = simulering(
            posteringer = nonEmptyListOf(
                postering(fraOgMed = 13.januar(2025), beløp = 255),
                postering(fraOgMed = 14.januar(2025), beløp = 255),
            ),
        )
        val nyForm = simulering(
            posteringer = nonEmptyListOf(
                postering(fraOgMed = 13.januar(2025), tilOgMed = 14.januar(2025), beløp = 510),
            ),
        )

        val ulikheter = gammelForm.finnUlikheter(nyForm)

        ulikheter.size shouldBe 1
        ulikheter.single() shouldContain "Kun i beregnet"
        ulikheter.single() shouldContain "Kun i kontroll"
    }

    @Test
    fun `ulike meldeperioder beskrives med begge ider`() {
        val annenMeldeperiode = meldeperiode(periode = periode)
        val kontroll = simulering(meldeperiode = annenMeldeperiode)

        simulering().finnUlikheter(kontroll) shouldBe listOf(
            "Ulike meldeperioder: beregnet=${meldeperiode.id}, kontroll=${annenMeldeperiode.id}",
        )
    }
}
