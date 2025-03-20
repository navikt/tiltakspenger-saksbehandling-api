package no.nav.tiltakspenger.saksbehandling.domene.meldekort.beregning

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DayOfWeek
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingType
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.beregn
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class MeldekortberegningKorrigeringTest {
    private val førsteDag = LocalDate.of(2025, 1, 6)
    private val førstePeriode = Periode(fraOgMed = førsteDag, tilOgMed = førsteDag.plusDays(13))

    private val deltatt10Dager =
        (0L..13L).map { index ->
            val dag = førsteDag.plusDays(index)
            val status = when (dag.dayOfWeek) {
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> SendMeldekortTilBeslutningKommando.Status.IKKE_DELTATT
                else -> SendMeldekortTilBeslutningKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
            }

            SendMeldekortTilBeslutningKommando.Dager.Dag(
                dag = dag,
                status = status,
            )
        }.toNonEmptyListOrNull()!!

    private fun forventetBeregning(
        dager: NonEmptyList<SendMeldekortTilBeslutningKommando.Dager.Dag>,
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ) = dager.map {
        when (it.status) {
            SendMeldekortTilBeslutningKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattUtenLønnITiltaket.create(
                meldekortId = meldekortId,
                dato = it.dag,
                antallBarn = antallBarn,
                tiltakstype = tiltakstype,
            )

            else -> MeldeperiodeBeregningDag.Utfylt.IkkeDeltatt.create(
                meldekortId = meldekortId,
                dato = it.dag,
                antallBarn = antallBarn,
                tiltakstype = tiltakstype,
            )
        }
    }

    @Test
    fun `Skal beregne for korrigering`() {
        val sakId = SakId.random()

        val meldekortBehandlet1 = ObjectMother.meldekortBehandlet(sakId = sakId, periode = førstePeriode)
        val meldekortBehandlet2 = ObjectMother.meldekortBehandlet(sakId = sakId, periode = førstePeriode.plus14Dager())

        val meldekortKorrigert = ObjectMother.meldekortUnderBehandling(
            sakId = sakId,
            periode = førstePeriode,
            type = MeldekortBehandlingType.KORRIGERING,
        )

        val kommando = SendMeldekortTilBeslutningKommando(
            sakId = sakId,
            meldekortId = meldekortKorrigert.id,
            saksbehandler = ObjectMother.saksbehandler(navIdent = meldekortKorrigert.saksbehandler),
            correlationId = CorrelationId.generate(),
            dager = SendMeldekortTilBeslutningKommando.Dager(deltatt10Dager),
        )

        val beregning = kommando.beregn(
            barnetilleggsPerioder = Periodisering(),
            tiltakstypePerioder = Periodisering(
                PeriodeMedVerdi(
                    periode = Periode(fraOgMed = førsteDag, tilOgMed = førsteDag.plusDays(100)),
                    verdi = TiltakstypeSomGirRett.JOBBKLUBB,
                ),
            ),
            eksisterendeMeldekortBehandlinger = MeldekortBehandlinger(
                listOf(meldekortBehandlet1, meldekortBehandlet2, meldekortKorrigert),
            ),
        )

        beregning shouldBe forventetBeregning(
            deltatt10Dager,
            meldekortKorrigert.id,
            TiltakstypeSomGirRett.JOBBKLUBB,
            AntallBarn(0),
        )
    }
}
