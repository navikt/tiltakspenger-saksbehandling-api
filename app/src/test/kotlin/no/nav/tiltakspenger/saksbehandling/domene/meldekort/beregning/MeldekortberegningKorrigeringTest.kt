package no.nav.tiltakspenger.saksbehandling.domene.meldekort.beregning

import arrow.core.toNonEmptyListOrNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DayOfWeek
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingType
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Satser
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class MeldekortberegningKorrigeringTest {
    private val førsteDag = LocalDate.of(2025, 1, 6)
    private val førstePeriode = Periode(fraOgMed = førsteDag, tilOgMed = førsteDag.plusDays(13))

    private val sats2025 = Satser.sats(førsteDag).sats

    private val deltatt10Dager =
        (0L..13L).map { index ->
            val dag = førsteDag.plusDays(index)
            val status = when (dag.dayOfWeek) {
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> SendMeldekortTilBeslutningKommando.Status.SPERRET
                else -> SendMeldekortTilBeslutningKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
            }

            SendMeldekortTilBeslutningKommando.Dager.Dag(
                dag = dag,
                status = status,
            )
        }.toNonEmptyListOrNull()!!

    private val deltatt5DagerUgyldigFravær5Dager =
        (0L..13L).map { index ->
            val dag = førsteDag.plusDays(index)
            val status = when (dag.dayOfWeek) {
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> SendMeldekortTilBeslutningKommando.Status.SPERRET
                else -> if (index > 6) {
                    SendMeldekortTilBeslutningKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
                } else {
                    SendMeldekortTilBeslutningKommando.Status.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
                }
            }

            SendMeldekortTilBeslutningKommando.Dager.Dag(
                dag = dag,
                status = status,
            )
        }.toNonEmptyListOrNull()!!

    @Test
    fun `Skal beregne for korrigering`() {
        val sakId = SakId.random()

        val barnetilleggsPerioder = Periodisering<AntallBarn?>()
        val tiltakstypePerioder = Periodisering<TiltakstypeSomGirRett?>(
            PeriodeMedVerdi(
                periode = Periode(fraOgMed = førsteDag, tilOgMed = førsteDag.plusDays(100)),
                verdi = TiltakstypeSomGirRett.GRUPPE_AMO,
            ),
        )

        val meldekortFørstegangsBehandling = ObjectMother.meldekortUnderBehandling(
            sakId = sakId,
            periode = førstePeriode,
            type = MeldekortBehandlingType.FØRSTE_BEHANDLING,
        )

        val meldekortBehandlingerMedFørsteUnderBehandling = MeldekortBehandlinger(
            listOf(meldekortFørstegangsBehandling),
        )

        val førsteBehandlingKommando = SendMeldekortTilBeslutningKommando(
            sakId = sakId,
            meldekortId = meldekortFørstegangsBehandling.id,
            saksbehandler = ObjectMother.saksbehandler(navIdent = meldekortFørstegangsBehandling.saksbehandler),
            correlationId = CorrelationId.generate(),
            dager = SendMeldekortTilBeslutningKommando.Dager(deltatt10Dager),
            meldekortbehandlingBegrunnelse = null,
        )

        val meldekortFørstegangsBehandlet = meldekortBehandlingerMedFørsteUnderBehandling.sendTilBeslutter(
            førsteBehandlingKommando,
            barnetilleggsPerioder,
            tiltakstypePerioder,
            fixedClock,
        ).getOrFail().second

        meldekortFørstegangsBehandlet.beløpTotal shouldBe sats2025 * 10

        val meldekortKorrigering = ObjectMother.meldekortUnderBehandling(
            sakId = sakId,
            periode = førstePeriode,
            type = MeldekortBehandlingType.KORRIGERING,
        )

        val korrigeringKommando = SendMeldekortTilBeslutningKommando(
            sakId = sakId,
            meldekortId = meldekortKorrigering.id,
            saksbehandler = ObjectMother.saksbehandler(navIdent = meldekortKorrigering.saksbehandler),
            correlationId = CorrelationId.generate(),
            dager = SendMeldekortTilBeslutningKommando.Dager(deltatt5DagerUgyldigFravær5Dager),
            meldekortbehandlingBegrunnelse = null,
        )

        val meldekortBehandlingerMedKorrigeringUnderBehandling = MeldekortBehandlinger(
            listOf(meldekortFørstegangsBehandlet, meldekortKorrigering),
        )

        val meldekortKorrigeringBehandlet = meldekortBehandlingerMedKorrigeringUnderBehandling.sendTilBeslutter(
            korrigeringKommando,
            barnetilleggsPerioder,
            tiltakstypePerioder,
            fixedClock,
        ).getOrFail().second

        meldekortKorrigeringBehandlet.beløpTotal shouldBe sats2025 * 5
    }
}
