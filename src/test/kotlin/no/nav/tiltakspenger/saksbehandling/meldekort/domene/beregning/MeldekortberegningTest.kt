package no.nav.tiltakspenger.saksbehandling.meldekort.domene.beregning

import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.satser.Satsdag
import no.nav.tiltakspenger.libs.satser.Satser
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.oppdater.OppdaterMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilMeldeperiodeBeregninger
import org.junit.jupiter.api.Test
import java.time.LocalDate

private typealias Dag = OppdaterMeldekortbehandlingKommando.OppdatertMeldeperiode.OppdatertDag
private typealias Status = OppdaterMeldekortbehandlingKommando.Status

internal class MeldekortberegningTest {
    private val førsteDag = LocalDate.of(2024, 1, 1)
    private val vedtaksperiode = Periode(fraOgMed = førsteDag, tilOgMed = førsteDag.plusDays(364))

    private fun periodeMedStatuser(fraDato: LocalDate, vararg statuser: List<Status>) =
        statuser.toList().flatten()
            .mapIndexed { index, status -> Dag(fraDato.plusDays(index.toLong()), status) }
            .toNonEmptyListOrNull()!!

    private fun periodeMedFullDeltakelse(fraDato: LocalDate) = periodeMedStatuser(
        fraDato,
        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
    )

    private fun sumAv(full: Int = 0, redusert: Int = 0, barn: Int = 0, satser: Satsdag = Satser.sats(førsteDag)) =
        full * satser.sats + redusert * satser.satsRedusert +
            barn * (full * satser.satsBarnetillegg + redusert * satser.satsBarnetilleggRedusert)

    @Test
    fun `Skal korrigere en behandling`() {
        runTest {
            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                vedtaksperiode = vedtaksperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedFullDeltakelse(førsteDag),
                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.FRAVÆR_ANNET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),
                ),
            )

            meldekortbehandlinger.sisteGodkjenteMeldekort!!.beløpTotal shouldBe sumAv(
                full = 5,
            )
        }
    }

    @Test
    fun `Skal korrigere en tidligere behandling`() {
        runTest {
            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                vedtaksperiode = vedtaksperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedFullDeltakelse(førsteDag),
                    periodeMedFullDeltakelse(førsteDag.plusWeeks(2)),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.FRAVÆR_ANNET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),
                ),
            )

            meldekortbehandlinger.sisteBehandledeMeldekortPerKjede[0].beløpTotal shouldBe sumAv(
                full = 5,
            )
        }
    }

    @Test
    fun `Skal beregne sykedager frem i tid ved korrigering`() {
        runTest {
            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                vedtaksperiode = vedtaksperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedFullDeltakelse(førsteDag),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(3) { Status.FRAVÆR_SYK },
                        List(2) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),
                ),
            )

            meldekortbehandlinger.sisteBehandledeMeldekortPerKjede[0].beløpTotal shouldBe sumAv(
                full = 8 + 7,
                redusert = 2 + 3,
            )
        }
    }

    @Test
    fun `Skal beregne sykedager to perioder frem i tid ved korrigering til 0-beløp`() {
        runTest {
            val clock = TikkendeKlokke()
            val sisteMeldeperiode = førsteDag.plusWeeks(4) til førsteDag.plusWeeks(4).plusDays(13)
            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                clock = clock,
                vedtaksperiode = vedtaksperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedStatuser(
                        førsteDag,
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(3) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    periodeMedStatuser(
                        sisteMeldeperiode.fraOgMed,
                        List(1) { Status.FRAVÆR_SYK },
                        List(13) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(2) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(1) { Status.FRAVÆR_SYK },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),
                ),
            )

            val korrigeringAvFørsteMeldekort = meldekortbehandlinger.sisteBehandledeMeldekortPerKjede.first()

            // Skal inkludere alle beregninger frem til siste korrigerte meldeperiode
            korrigeringAvFørsteMeldekort.beregning.size shouldBe 3

            val beregninger = meldekortbehandlinger.tilMeldeperiodeBeregninger(clock)

            beregninger.gjeldendeBeregningPerKjede[MeldeperiodeKjedeId.fraPeriode(sisteMeldeperiode)]!!.totalBeløp shouldBe 0
        }
    }

    @Test
    fun `Skal beregne sykedager frem i tid ved korrigering med barnetillegg`() {
        runTest {
            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                vedtaksperiode = vedtaksperiode,
                barnetilleggsPerioder = SammenhengendePeriodisering(
                    AntallBarn(2),
                    vedtaksperiode,
                ),
                meldeperioder = nonEmptyListOf(
                    periodeMedFullDeltakelse(førsteDag),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(3) { Status.FRAVÆR_SYK },
                        List(2) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),
                ),
            )

            meldekortbehandlinger.sisteBehandledeMeldekortPerKjede[0].beløpTotal shouldBe sumAv(
                full = 8 + 7,
                redusert = 2 + 3,
                barn = 2,
            )
        }
    }

    @Test
    fun `Arbeidsgiverperiode skal ikke resettes ved sykedager med 15 dager mellom`() {
        runTest {
            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                vedtaksperiode = vedtaksperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(2) { Status.FRAVÆR_SYK },
                        List(3) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(4) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(1) { Status.FRAVÆR_SYK },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(3) { Status.FRAVÆR_SYK },
                        List(2) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),
                ),
            )

            meldekortbehandlinger.sisteBehandledeMeldekortPerKjede[0].beløpTotal shouldBe sumAv(
                full = 10 + 9,
                redusert = 1,
            )
        }
    }

    @Test
    fun `Arbeidsgiverperiode skal resettes med 16 dager mellom sykedager`() {
        runTest {
            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                vedtaksperiode = vedtaksperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(2) { Status.FRAVÆR_SYK },
                        List(3) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(4) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(1) { Status.FRAVÆR_SYK },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),
                ),
            )

            meldekortbehandlinger.sisteBehandledeMeldekortPerKjede[0].beløpTotal shouldBe sumAv(
                full = 10,
            )
        }
    }

    @Test
    fun `Skal beregne sykedager med riktige satser rundt årsskifte`() {
        runTest {
            val clock = TikkendeKlokke(fixedClockAt(1.februar(2025)))
            val førsteDag = 23.desember(2024)

            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                clock = clock,
                vedtaksperiode = Periode(førsteDag, førsteDag.plusDays(100)),
                meldeperioder = nonEmptyListOf(
                    periodeMedFullDeltakelse(førsteDag),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(3) { Status.FRAVÆR_SYK },
                        List(2) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),
                ),
            )

            meldekortbehandlinger.sisteBehandledeMeldekortPerKjede[0].beløpTotal shouldBe sumAv(
                full = 7,
                satser = Satser.sats(førsteDag),
            ) + sumAv(
                full = 1 + 7,
                redusert = 2 + 3,
                satser = Satser.sats(LocalDate.of(2025, 1, 1)),
            )
        }
    }

    @Test
    fun `Skal korrigere en korrigering`() {
        runTest {
            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                vedtaksperiode = vedtaksperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedFullDeltakelse(førsteDag),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(3) { Status.FRAVÆR_SYK },
                        List(2) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(1) { Status.FRAVÆR_SYK },
                        List(4) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),
                ),
            )

            meldekortbehandlinger.sisteBehandledeMeldekortPerKjede[0].beløpTotal shouldBe sumAv(
                full = 8 + 7,
                redusert = 2 + 3,
            )
        }
    }

    @Test
    fun `Skal korrigere en korrigering, med tidligere korrigering i en påfølgende periode`() {
        runTest {
            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                vedtaksperiode = vedtaksperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedFullDeltakelse(førsteDag),
                    periodeMedFullDeltakelse(førsteDag.plusWeeks(2)),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(4) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(1) { Status.FRAVÆR_SYKT_BARN },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.FRAVÆR_SYKT_BARN },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.FRAVÆR_SYKT_BARN },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),
                ),
            )

            meldekortbehandlinger.sisteBehandledeMeldekortPerKjede[0].beløpTotal shouldBe sumAv(
                full = 8 + 5,
                redusert = 2 + 5,
            )
        }
    }

    @Test
    fun `Skal korrigere en korrigering, som fører til opprinnelig beregning av neste periode`() {
        runTest {
            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                vedtaksperiode = vedtaksperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedFullDeltakelse(førsteDag),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(2) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(3) { Status.FRAVÆR_SYKT_BARN },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(4) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(1) { Status.FRAVÆR_SYKT_BARN },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    periodeMedFullDeltakelse(førsteDag),
                ),
            )

            meldekortbehandlinger.sisteBehandledeMeldekortPerKjede[0].beløpTotal shouldBe sumAv(
                full = 20,
            )
        }
    }

    @Test
    fun `Skal beregne meldeperioder med hull mellom seg`() {
        runTest {
            val clock = TikkendeKlokke()

            // Kun periode 1 og 3 finnes - periode 2 er et hull som aldri behandles
            val periode1 = førsteDag
            val periode3 = førsteDag.plusWeeks(4)

            fun kjedeId(fraDato: LocalDate) =
                MeldeperiodeKjedeId.fraPeriode(Periode(fraDato, fraDato.plusDays(13)))

            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                clock = clock,
                vedtaksperiode = vedtaksperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedFullDeltakelse(periode1),
                    periodeMedFullDeltakelse(periode3),

                    // Korriger periode 1 -> 5 dager med rett
                    periodeMedStatuser(
                        periode1,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.FRAVÆR_ANNET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),
                ),
            )

            val beregninger = meldekortbehandlinger.tilMeldeperiodeBeregninger(clock)

            beregninger.gjeldendeBeregningPerKjede[kjedeId(periode1)]!!.totalBeløp shouldBe sumAv(full = 5)
            beregninger.gjeldendeBeregningPerKjede[kjedeId(periode3)]!!.totalBeløp shouldBe sumAv(full = 10)
        }
    }

    @Test
    fun `Skal korrigere ikke-sammenhengende meldeperioder - periode 1 og 3 av 3`() {
        runTest {
            val clock = TikkendeKlokke()

            val periode1 = førsteDag
            val periode2 = førsteDag.plusWeeks(2)
            val periode3 = førsteDag.plusWeeks(4)

            fun kjedeId(fraDato: LocalDate) =
                MeldeperiodeKjedeId.fraPeriode(Periode(fraDato, fraDato.plusDays(13)))

            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                clock = clock,
                vedtaksperiode = vedtaksperiode,
                meldeperioder = nonEmptyListOf(
                    // 3 initielle meldeperioder med full deltakelse (10 dager med rett hver)
                    periodeMedFullDeltakelse(periode1),
                    periodeMedFullDeltakelse(periode2),
                    periodeMedFullDeltakelse(periode3),

                    // Korriger periode 1 -> 5 dager med rett
                    periodeMedStatuser(
                        periode1,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.FRAVÆR_ANNET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    // Korriger periode 3 -> 5 dager med rett (hull ved periode 2, som ikke korrigeres)
                    periodeMedStatuser(
                        periode3,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.FRAVÆR_ANNET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),
                ),
            )

            val beregninger = meldekortbehandlinger.tilMeldeperiodeBeregninger(clock)

            // Periode 1 og 3 er korrigert, periode 2 er urørt
            beregninger.gjeldendeBeregningPerKjede[kjedeId(periode1)]!!.totalBeløp shouldBe sumAv(full = 5)
            beregninger.gjeldendeBeregningPerKjede[kjedeId(periode2)]!!.totalBeløp shouldBe sumAv(full = 10)
            beregninger.gjeldendeBeregningPerKjede[kjedeId(periode3)]!!.totalBeløp shouldBe sumAv(full = 5)
        }
    }

    @Test
    fun `Skal beregne reduksjon for en korrigert senere periode basert på sykedager fra en tidligere periode`() {
        runTest {
            val clock = TikkendeKlokke()

            val periode1 = førsteDag
            val periode2 = førsteDag.plusWeeks(2)

            fun kjedeId(fraDato: LocalDate) =
                MeldeperiodeKjedeId.fraPeriode(Periode(fraDato, fraDato.plusDays(13)))

            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                clock = clock,
                vedtaksperiode = vedtaksperiode,
                meldeperioder = nonEmptyListOf(
                    // Periode 1: 5 sykedager -> sykedagtelleren står på 5 ved slutten
                    // (siste sykedag = førsteDag+11)
                    periodeMedStatuser(
                        periode1,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),

                    // Periode 2: opprinnelig full deltakelse
                    periodeMedFullDeltakelse(periode2),

                    // Korriger periode 2 til sykedager. Reduksjonen avhenger av at
                    // sykedagtelleren fra periode 1 - som IKKE beregnes på nytt her - spilles
                    // av på nytt (tidligereMeldeperioder) for å sette riktig state.
                    periodeMedStatuser(
                        periode2,
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),
                ),
            )

            val beregninger = meldekortbehandlinger.tilMeldeperiodeBeregninger(clock)

            // Telleren fortsetter fra 5 (periode 1, kun 3 dager unna) -> alle 5 sykedagene i
            // periode 2 blir redusert. Uten avspilling ville de 3 første vært uten reduksjon
            // (full sats), altså sumAv(full = 8, redusert = 2).
            beregninger.gjeldendeBeregningPerKjede[kjedeId(periode2)]!!.totalBeløp shouldBe
                sumAv(full = 5, redusert = 5)
        }
    }

    @Test
    fun `Skal videreføre sykedagtelleren over et hull i meldeperiodene når det er 16 dager eller mindre mellom sykedagene`() {
        runTest {
            val clock = TikkendeKlokke()

            // Kun periode 1 og 3 finnes - periode 2 er et hull
            val periode1 = førsteDag
            val periode3 = førsteDag.plusWeeks(4)

            fun kjedeId(fraDato: LocalDate) =
                MeldeperiodeKjedeId.fraPeriode(Periode(fraDato, fraDato.plusDays(13)))

            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                clock = clock,
                vedtaksperiode = vedtaksperiode,
                meldeperioder = nonEmptyListOf(
                    // Periode 1 ender med 5 sykedager på de siste dagene (siste sykedag = førsteDag+13),
                    // slik at telleren står på 5.
                    periodeMedStatuser(
                        periode1,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(4) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.FRAVÆR_SYK },
                    ),

                    // Periode 3 starter på førsteDag+28 med sykedager fra første dag. Første sykedag er
                    // da 15 dager etter siste sykedag i periode 1, altså <= 16 dager -> telleren
                    // videreføres over hullet (ingen nullstilling). Dette krever at periode 1 spilles
                    // av på nytt (tidligereMeldeperioder) selv om den ligger på andre siden av hullet.
                    periodeMedStatuser(
                        periode3,
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.IKKE_RETT_TIL_TILTAKSPENGER },
                    ),
                ),
            )

            val beregninger = meldekortbehandlinger.tilMeldeperiodeBeregninger(clock)

            // Telleren fortsetter fra 5 -> alle 5 sykedagene i periode 3 blir redusert.
            // Uten videreføring over hullet ville de 3 første vært uten reduksjon (full sats),
            // altså sumAv(full = 8, redusert = 2).
            beregninger.gjeldendeBeregningPerKjede[kjedeId(periode3)]!!.totalBeløp shouldBe
                sumAv(full = 5, redusert = 5)
        }
    }
}
