package no.nav.tiltakspenger.saksbehandling.domene.meldekort.beregning

import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Satsdag
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Satser
import org.junit.jupiter.api.Test
import java.time.LocalDate

private typealias Dag = OppdaterMeldekortKommando.Dager.Dag
private typealias Status = OppdaterMeldekortKommando.Status

internal class MeldekortberegningKorrigeringTest {
    private val førsteDag = LocalDate.of(2024, 1, 1)
    private val vurderingsperiode = Periode(fraOgMed = førsteDag, tilOgMed = førsteDag.plusDays(364))

    private fun periodeMedStatuser(fraDato: LocalDate, vararg statuser: List<Status>) =
        statuser.toList().flatten()
            .mapIndexed { index, status -> Dag(fraDato.plusDays(index.toLong()), status) }
            .toNonEmptyListOrNull()!!

    private fun periodeMedFullDeltagelse(fraDato: LocalDate) = periodeMedStatuser(
        fraDato,
        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
        List(2) { Status.SPERRET },
        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
        List(2) { Status.SPERRET },
    )

    private fun sumAv(full: Int = 0, redusert: Int = 0, barn: Int = 0, satser: Satsdag = Satser.sats(førsteDag)) =
        full * satser.sats + redusert * satser.satsRedusert +
            barn * (full * satser.satsBarnetillegg + redusert * satser.satsBarnetilleggRedusert)

    @Test
    fun `Skal korrigere en behandling`() {
        runTest {
            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                vurderingsperiode = vurderingsperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedFullDeltagelse(førsteDag),
                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(5) { Status.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV },
                        List(2) { Status.SPERRET },
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
                vurderingsperiode = vurderingsperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedFullDeltagelse(førsteDag),
                    periodeMedFullDeltagelse(førsteDag.plusWeeks(2)),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV },
                        List(2) { Status.SPERRET },
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
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
                vurderingsperiode = vurderingsperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedFullDeltagelse(førsteDag),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(3) { Status.FRAVÆR_SYK },
                        List(2) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.SPERRET },
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
            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                vurderingsperiode = vurderingsperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedStatuser(
                        førsteDag,
                        List(2) { Status.SPERRET },
                        List(3) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.SPERRET },
                    ),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.SPERRET },
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.SPERRET },
                    ),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(4),
                        List(1) { Status.FRAVÆR_SYK },
                        List(13) { Status.SPERRET },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(2) { Status.SPERRET },
                        List(2) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(1) { Status.FRAVÆR_SYK },
                        List(2) { Status.SPERRET },
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.SPERRET },
                    ),
                ),
            )

            val sisteKjedeId = meldekortbehandlinger.last().kjedeId

            meldekortbehandlinger.meldeperiodeBeregninger.sisteBeregningForKjede[sisteKjedeId]!!.beregnTotaltBeløp() shouldBe 0
        }
    }

    @Test
    fun `Skal beregne sykedager frem i tid ved korrigering med barnetillegg`() {
        runTest {
            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                vurderingsperiode = vurderingsperiode,
                barnetilleggsPerioder = Periodisering(
                    AntallBarn(2),
                    vurderingsperiode,
                ),
                meldeperioder = nonEmptyListOf(
                    periodeMedFullDeltagelse(førsteDag),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(3) { Status.FRAVÆR_SYK },
                        List(2) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.SPERRET },
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
                vurderingsperiode = vurderingsperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(2) { Status.FRAVÆR_SYK },
                        List(3) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                    ),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(4) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(1) { Status.FRAVÆR_SYK },
                        List(2) { Status.SPERRET },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(3) { Status.FRAVÆR_SYK },
                        List(2) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
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
                vurderingsperiode = vurderingsperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(2) { Status.FRAVÆR_SYK },
                        List(3) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                    ),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(4) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(1) { Status.FRAVÆR_SYK },
                        List(2) { Status.SPERRET },
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
            val førsteDag = LocalDate.of(2024, 12, 23)

            val meldekortbehandlinger = ObjectMother.beregnMeldekortperioder(
                vurderingsperiode = Periode(førsteDag, førsteDag.plusDays(100)),
                meldeperioder = nonEmptyListOf(
                    periodeMedFullDeltagelse(førsteDag),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(3) { Status.FRAVÆR_SYK },
                        List(2) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.SPERRET },
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
                vurderingsperiode = vurderingsperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedFullDeltagelse(førsteDag),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(3) { Status.FRAVÆR_SYK },
                        List(2) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(1) { Status.FRAVÆR_SYK },
                        List(4) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(5) { Status.FRAVÆR_SYK },
                        List(2) { Status.SPERRET },
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
                vurderingsperiode = vurderingsperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedFullDeltagelse(førsteDag),
                    periodeMedFullDeltagelse(førsteDag.plusWeeks(2)),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(4) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(1) { Status.FRAVÆR_SYKT_BARN },
                        List(2) { Status.SPERRET },
                    ),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(5) { Status.FRAVÆR_SYKT_BARN },
                        List(2) { Status.SPERRET },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(5) { Status.FRAVÆR_SYKT_BARN },
                        List(2) { Status.SPERRET },
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
                vurderingsperiode = vurderingsperiode,
                meldeperioder = nonEmptyListOf(
                    periodeMedFullDeltagelse(førsteDag),

                    periodeMedStatuser(
                        førsteDag.plusWeeks(2),
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(2) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(3) { Status.FRAVÆR_SYKT_BARN },
                        List(2) { Status.SPERRET },
                    ),

                    periodeMedStatuser(
                        førsteDag,
                        List(5) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(2) { Status.SPERRET },
                        List(4) { Status.DELTATT_UTEN_LØNN_I_TILTAKET },
                        List(1) { Status.FRAVÆR_SYKT_BARN },
                        List(2) { Status.SPERRET },
                    ),

                    periodeMedFullDeltagelse(førsteDag),
                ),
            )

            meldekortbehandlinger.sisteBehandledeMeldekortPerKjede[0].beløpTotal shouldBe sumAv(
                full = 20,
            )
        }
    }
}
