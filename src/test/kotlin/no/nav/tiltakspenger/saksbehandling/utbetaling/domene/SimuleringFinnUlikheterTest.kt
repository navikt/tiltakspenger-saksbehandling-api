package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.clock
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.meldeperiode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SimuleringFinnUlikheterTest {
    private val periode = Periode(13.januar(2025), 26.januar(2025))
    private val meldeperiode = meldeperiode(periode = periode)

    private fun simulering(
        meldeperiode: Meldeperiode = this.meldeperiode,
        tidligereUtbetalt: Int = 0,
        nyUtbetaling: Int = 255,
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
                ),
            ),
            simuleringstidspunkt = LocalDateTime.now(clock),
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
            posteringsdag = PosteringerForDag(
                dato = dato,
                posteringer = nonEmptyListOf(
                    PosteringForDag(
                        dato = dato,
                        fagområde = "TILTAKSPENGER",
                        beløp = nyUtbetaling,
                        type = Posteringstype.YTELSE,
                        klassekode = "test_klassekode",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `like simuleringer gir ingen ulikheter`() {
        simulering().finnUlikheter(simulering()) shouldBe emptyList()
        simulering().erLik(simulering()) shouldBe true
        (null as Simulering?).finnUlikheter(null) shouldBe emptyList()
    }

    @Test
    fun `manglende simulering mot endring beskriver begge sider`() {
        val kontroll = simulering(tidligereUtbetalt = 0, nyUtbetaling = 255)

        (null as Simulering?).finnUlikheter(kontroll) shouldBe listOf(
            "Ulike simuleringstyper: beregnet=mangler, kontroll=endring (totalPeriode=$periode, tidligereUtbetalt=0, nyUtbetaling=255, totalEtterbetaling=0, totalFeilutbetaling=0, totalJustering=0, totalTrekk=0)",
        )
    }

    @Test
    fun `endrede beløp beskrives per dag og felt`() {
        val beregnet = simulering(tidligereUtbetalt = 0, nyUtbetaling = 255)
        val kontroll = simulering(tidligereUtbetalt = 255, nyUtbetaling = 510)

        beregnet.finnUlikheter(kontroll) shouldBe listOf(
            "Meldeperiode ${meldeperiode.id} ${periode.fraOgMed} (beregnet->kontroll): tidligereUtbetalt 0->255, nyUtbetaling 255->510",
        )
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
