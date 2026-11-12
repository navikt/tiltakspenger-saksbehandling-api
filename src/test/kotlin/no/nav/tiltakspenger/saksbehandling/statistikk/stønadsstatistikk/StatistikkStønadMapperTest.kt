package no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgetTiltaksdeltakelseKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class StatistikkStønadMapperTest {

    @Test
    fun `innvilgelsesperiode med flere tiltaksdeltakelser gir alle i listen og den første som enkeltverdi`() = runTest {
        val periode = 1.januar(2023) til 31.mars(2023)
        val første = ObjectMother.tiltaksdeltakelse(periode = periode)
        val andre = ObjectMother.tiltaksdeltakelse(periode = periode)
        val innvilgelsesperioder = listOf(
            InnvilgelsesperiodeKommando(
                periode = periode,
                tiltaksdeltakelser = nonEmptyListOf(
                    InnvilgetTiltaksdeltakelseKommando(første.internDeltakelseId, AntallDagerForMeldeperiode(6)),
                    InnvilgetTiltaksdeltakelseKommando(andre.internDeltakelseId, AntallDagerForMeldeperiode(4)),
                ),
            ),
        )
        val sakId = SakId.random()
        val vedtak = ObjectMother.nyRammevedtakInnvilgelse(
            sakId = sakId,
            innvilgelsesperioder = innvilgelsesperioder,
            behandling = ObjectMother.nyVedtattSøknadsbehandling(
                sakId = sakId,
                saksopplysningsperiode = periode,
                saksopplysninger = ObjectMother.saksopplysninger(
                    fom = periode.fraOgMed,
                    tom = periode.tilOgMed,
                    tiltaksdeltakelse = listOf(første, andre),
                ),
                innvilgelsesperioder = innvilgelsesperioder,
            ),
        )

        val statistikk = genererStønadsstatistikkForRammevedtak(vedtak).genererStønadsstatistikk()

        statistikk.innvilgelsesperioder shouldBe listOf(
            StatistikkStønadDTO.InnvilgelsesperiodeStatistikk(
                fraOgMed = periode.fraOgMed,
                tilOgMed = periode.tilOgMed,
                tiltaksdeltakelse = første.eksternDeltakelseId,
                tiltaksdeltakelser = listOf(første.eksternDeltakelseId, andre.eksternDeltakelseId),
            ),
        )
    }
}
