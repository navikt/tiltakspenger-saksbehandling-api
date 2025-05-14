package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjede
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppsummeringGeneratorTest {

    @Test
    fun `enkel YTELSE for en meldeperiode`() {
        // Meldeperiode mandag 14. oktober til søndag 27. oktober 2024
        val detaljer = Simulering.Endring.Detaljer(
            datoBeregnet = LocalDate.parse("2025-05-12"),
            totalBeløp = 2280,
            perioder = nonEmptyListOf(
                Simulering.Endring.Detaljer.Simuleringsperiode(
                    periode = Periode(LocalDate.parse("2024-10-14"), LocalDate.parse("2024-10-17")),
                    delperioder = listOf(
                        Simulering.Endring.Detaljer.Simuleringsperiode.Delperiode(
                            fagområde = "TILTAKSPENGER",
                            periode = Periode(LocalDate.parse("2024-10-14"), LocalDate.parse("2024-10-17")),
                            beløp = 1140,
                            type = Simulering.Endring.PosteringType.YTELSE,
                            klassekode = "TPTPATT",
                        ),
                    ),
                ),
                Simulering.Endring.Detaljer.Simuleringsperiode(
                    periode = Periode(LocalDate.parse("2024-10-21"), LocalDate.parse("2024-10-22")),
                    delperioder = listOf(
                        Simulering.Endring.Detaljer.Simuleringsperiode.Delperiode(
                            fagområde = "TILTAKSPENGER",
                            periode = Periode(LocalDate.parse("2024-10-21"), LocalDate.parse("2024-10-22")),
                            beløp = 570,
                            type = Simulering.Endring.PosteringType.YTELSE,
                            klassekode = "TPTPATT",
                        ),
                    ),
                ),
                Simulering.Endring.Detaljer.Simuleringsperiode(
                    periode = Periode(LocalDate.parse("2024-10-24"), LocalDate.parse("2024-10-25")),
                    delperioder = listOf(
                        Simulering.Endring.Detaljer.Simuleringsperiode.Delperiode(
                            fagområde = "TILTAKSPENGER",
                            periode = Periode(LocalDate.parse("2024-10-24"), LocalDate.parse("2024-10-25")),
                            beløp = 570,
                            type = Simulering.Endring.PosteringType.YTELSE,
                            klassekode = "TPTPATT",
                        ),
                    ),
                ),
            ),
        )
        val meldeperiode = Periode(LocalDate.parse("2024-10-14"), LocalDate.parse("2024-10-27"))
        val meldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(
            meldeperiode,
        )
        val meldeperiodeKjeder = MeldeperiodeKjeder(
            MeldeperiodeKjede(
                ObjectMother.meldeperiode(
                    periode = meldeperiode,
                    kjedeId = meldeperiodeKjedeId,
                ),
            ),
        )
        OppsummeringGenerator.lagOppsummering(detaljer, meldeperiodeKjeder) shouldBe Simulering.Endring.Oppsummering(
            periode = Periode(LocalDate.parse("2024-10-14"), LocalDate.parse("2024-10-25")),
            tidligereUtbetalt = 0,
            nyUtbetaling = 2280,
            totalEtterbetaling = 2280,
            totalFeilutbetaling = 0,
            perMeldeperiode = nonEmptyListOf(
                Simulering.Endring.OppsummeringForMeldeperiode(
                    meldeperiode = meldeperiode,
                    meldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(meldeperiode),
                    tidligereUtbetalt = 0,
                    nyUtbetaling = 2280,
                    totalEtterbetaling = 2280,
                    totalFeilutbetaling = 0,
                ),
            ),
        )
    }

    @Test
    fun test() {
        /**
         * Dette er en buggy simulering/utbetaling pga. bug ved bruk av kode 7.
         * Fra dev. Deltatt 10 ukedager i første vedtak. Korrigerer 2. og 3. desember til deltatt med lønn (fra 100% ->0%)
         * Forventer minus 570 (2*285) kroner til feilkonto.
         *
         * Vi hadde forventet at det var tidligere utbetalt 1425 kroner i perioden 2. desember til 6. desember. (285*5).
         * Vi hadde forventet 2 dager feilutbetaling (2*285) i perioden 2. desember til 3. desember og tilhørende motpostering.
         */
        val detaljer = Simulering.Endring.Detaljer(
            datoBeregnet = LocalDate.parse("2025-05-12"),
            totalBeløp = 0,
            perioder = nonEmptyListOf(
                Simulering.Endring.Detaljer.Simuleringsperiode(
                    periode = Periode(LocalDate.parse("2024-12-02"), LocalDate.parse("2024-12-03")),
                    delperioder = listOf(
                        Simulering.Endring.Detaljer.Simuleringsperiode.Delperiode(
                            fagområde = "TILTAKSPENGER",
                            periode = Periode(LocalDate.parse("2024-12-02"), LocalDate.parse("2024-12-03")),
                            beløp = 570,
                            type = Simulering.Endring.PosteringType.YTELSE,
                            klassekode = "TPTPGRAMO",
                        ),
                        Simulering.Endring.Detaljer.Simuleringsperiode.Delperiode(
                            fagområde = "TILTAKSPENGER",
                            periode = Periode(LocalDate.parse("2024-12-02"), LocalDate.parse("2024-12-03")),
                            beløp = 570,
                            type = Simulering.Endring.PosteringType.FEILUTBETALING,
                            klassekode = "KL_KODE_FEIL_ARBYT",
                        ),
                        Simulering.Endring.Detaljer.Simuleringsperiode.Delperiode(
                            fagområde = "TILTAKSPENGER",
                            periode = Periode(LocalDate.parse("2024-12-02"), LocalDate.parse("2024-12-03")),
                            beløp = -570,
                            type = Simulering.Endring.PosteringType.MOTPOSTERING,
                            klassekode = "TBMOTOBS",
                        ),
                        Simulering.Endring.Detaljer.Simuleringsperiode.Delperiode(
                            fagområde = "TILTAKSPENGER",
                            periode = Periode(LocalDate.parse("2024-12-02"), LocalDate.parse("2024-12-03")),
                            beløp = -570,
                            type = Simulering.Endring.PosteringType.YTELSE,
                            klassekode = "TPTPGRAMO",
                        ),
                    ),
                ),
                Simulering.Endring.Detaljer.Simuleringsperiode(
                    periode = Periode(LocalDate.parse("2024-12-04"), LocalDate.parse("2024-12-06")),
                    delperioder = listOf(
                        Simulering.Endring.Detaljer.Simuleringsperiode.Delperiode(
                            fagområde = "TILTAKSPENGER",
                            periode = Periode(LocalDate.parse("2024-12-04"), LocalDate.parse("2024-12-06")),
                            beløp = 855,
                            type = Simulering.Endring.PosteringType.YTELSE,
                            klassekode = "TPTPGRAMO",
                        ),
                        Simulering.Endring.Detaljer.Simuleringsperiode.Delperiode(
                            fagområde = "TILTAKSPENGER",
                            periode = Periode(LocalDate.parse("2024-12-04"), LocalDate.parse("2024-12-06")),
                            beløp = -855,
                            type = Simulering.Endring.PosteringType.YTELSE,
                            klassekode = "TPTPGRAMO",
                        ),
                    ),
                ),
            ),
        )
        val meldeperiode1 = Periode(LocalDate.parse("2024-11-25"), LocalDate.parse("2024-12-08"))
        val meldeperiodeKjedeId1 = MeldeperiodeKjedeId.fraPeriode(meldeperiode1)
        val meldeperiodeKjeder = MeldeperiodeKjeder(
            listOf(
                MeldeperiodeKjede(
                    ObjectMother.meldeperiode(
                        periode = meldeperiode1,
                        kjedeId = meldeperiodeKjedeId1,
                    ),
                ),
            ),
        )
        OppsummeringGenerator.lagOppsummering(detaljer, meldeperiodeKjeder) shouldBe Simulering.Endring.Oppsummering(
            periode = Periode(LocalDate.parse("2024-12-02"), LocalDate.parse("2024-12-06")),
            tidligereUtbetalt = 1425,
            nyUtbetaling = 855,
            totalEtterbetaling = 0,
            totalFeilutbetaling = 570,
            perMeldeperiode = nonEmptyListOf(
                Simulering.Endring.OppsummeringForMeldeperiode(
                    meldeperiode = meldeperiode1,
                    meldeperiodeKjedeId = meldeperiodeKjedeId1,
                    tidligereUtbetalt = 1425,
                    nyUtbetaling = 855,
                    totalEtterbetaling = 0,
                    totalFeilutbetaling = 570,
                ),
            ),
        )
    }
}
