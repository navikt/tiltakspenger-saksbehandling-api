package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.felles.april
import no.nav.tiltakspenger.saksbehandling.felles.desember
import no.nav.tiltakspenger.saksbehandling.felles.februar
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.felles.mars
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vedtak.Vedtaksliste
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldeperiodeKjederTest {

    @Nested
    inner class FinnNærmesteMeldeperiodeForDato {
        @Test
        fun `tom kjede finner start dato for en kjede`() {
            val kjeder = MeldeperiodeKjeder(emptyList())
            kjeder.finnNærmesteMeldeperiode(2.januar(2023)) shouldBe Periode(2.januar(2023), 15.januar(2023))
        }

        @Test
        fun `angitt dato er 1 meldeperiode før kjedens meldeperiode`() {
            val kjeder = MeldeperiodeKjeder(
                MeldeperiodeKjede(ObjectMother.meldeperiode(periode = Periode(20.januar(2025), 2.februar(2025)))),
            )
            kjeder.finnNærmesteMeldeperiode(6.januar(2025)) shouldBe Periode(6.januar(2025), 19.januar(2025))
            kjeder.finnNærmesteMeldeperiode(19.januar(2025)) shouldBe Periode(6.januar(2025), 19.januar(2025))
        }

        @Test
        fun `angitt dato er 2 meldeperiode før kjedens meldeperiode`() {
            val kjeder = MeldeperiodeKjeder(
                MeldeperiodeKjede(ObjectMother.meldeperiode(periode = Periode(20.januar(2025), 2.februar(2025)))),
            )
            kjeder.finnNærmesteMeldeperiode(23.desember(2024)) shouldBe Periode(23.desember(2024), 5.januar(2025))
            kjeder.finnNærmesteMeldeperiode(5.januar(2025)) shouldBe Periode(23.desember(2024), 5.januar(2025))
        }

        @Test
        fun `angitt dato er etter meldeperioden`() {
            val kjeder = MeldeperiodeKjeder(
                MeldeperiodeKjede(ObjectMother.meldeperiode(periode = Periode(6.januar(2025), 19.januar(2025)))),
            )
            kjeder.finnNærmesteMeldeperiode(25.januar(2025)) shouldBe Periode(
                20.januar(2025),
                2.februar(2025),
            )
        }

        @Test
        fun `angitt dato er 1 meldeperiode etter kjedens-meldeperioden`() {
            val kjeder = MeldeperiodeKjeder(
                MeldeperiodeKjede(ObjectMother.meldeperiode(periode = Periode(20.januar(2025), 2.februar(2025)))),
            )
            kjeder.finnNærmesteMeldeperiode(3.februar(2025)) shouldBe Periode(3.februar(2025), 16.februar(2025))
            kjeder.finnNærmesteMeldeperiode(16.februar(2025)) shouldBe Periode(3.februar(2025), 16.februar(2025))
        }

        @Test
        fun `angitt dato er 2 meldeperiode etter kjedens-meldeperioden`() {
            val kjeder = MeldeperiodeKjeder(
                MeldeperiodeKjede(ObjectMother.meldeperiode(periode = Periode(20.januar(2025), 2.februar(2025)))),
            )
            kjeder.finnNærmesteMeldeperiode(17.februar(2025)) shouldBe Periode(17.februar(2025), 2.mars(2025))
            kjeder.finnNærmesteMeldeperiode(2.mars(2025)) shouldBe Periode(17.februar(2025), 2.mars(2025))
        }

        @Test
        fun `angitt dato er innenfor meldeperioden`() {
            val kjeder = MeldeperiodeKjeder(
                MeldeperiodeKjede(ObjectMother.meldeperiode(periode = Periode(6.januar(2025), 19.januar(2025)))),
            )
            kjeder.finnNærmesteMeldeperiode(6.januar(2025)) shouldBe Periode(6.januar(2025), 19.januar(2025))
            kjeder.finnNærmesteMeldeperiode(19.januar(2025)) shouldBe Periode(6.januar(2025), 19.januar(2025))
        }

        @Test
        fun `starten av en kjede for en sak`() {
            MeldeperiodeKjeder.finnNærmesteMeldeperiode(7.april(2024)) shouldBe Periode(1.april(2024), 14.april(2024))

            MeldeperiodeKjeder.finnNærmesteMeldeperiode(8.april(2024)) shouldBe Periode(8.april(2024), 21.april(2024))
            MeldeperiodeKjeder.finnNærmesteMeldeperiode(9.april(2024)) shouldBe Periode(8.april(2024), 21.april(2024))
            MeldeperiodeKjeder.finnNærmesteMeldeperiode(10.april(2024)) shouldBe Periode(8.april(2024), 21.april(2024))
            MeldeperiodeKjeder.finnNærmesteMeldeperiode(11.april(2024)) shouldBe Periode(8.april(2024), 21.april(2024))
            MeldeperiodeKjeder.finnNærmesteMeldeperiode(12.april(2024)) shouldBe Periode(8.april(2024), 21.april(2024))
            MeldeperiodeKjeder.finnNærmesteMeldeperiode(13.april(2024)) shouldBe Periode(8.april(2024), 21.april(2024))
            MeldeperiodeKjeder.finnNærmesteMeldeperiode(14.april(2024)) shouldBe Periode(8.april(2024), 21.april(2024))

            MeldeperiodeKjeder.finnNærmesteMeldeperiode(15.april(2024)) shouldBe Periode(15.april(2024), 28.april(2024))
        }
    }

    @Test
    fun `genererer første meldeperioder for et innvilgelse vedtak`() {
        val sakId = SakId.random()
        val periode = Periode(2.januar(2023), 17.januar(2023))
        val kjeder = MeldeperiodeKjeder(emptyList())
        val innvilgelseVedtak = ObjectMother.nyRammevedtakInnvilgelse(sakId = sakId, periode = periode)
        val actual = kjeder.genererMeldeperioder(Vedtaksliste(innvilgelseVedtak), LocalDate.MAX)

        val forventetFørstePeriode = Periode(2.januar(2023), 15.januar(2023))
        val forventetSistePeriode = Periode(16.januar(2023), 29.januar(2023))

        actual.let {
            it.first.meldeperioder shouldBe it.second

            it.first.first().periode shouldBe forventetFørstePeriode
            it.first.last().periode shouldBe forventetSistePeriode

            it.first.meldeperioder.first().antallDagerSomGirRett shouldBe 14
            it.first.meldeperioder.first().antallDagerForPeriode shouldBe 10

            it.first.meldeperioder.last().antallDagerSomGirRett shouldBe 2
            it.first.meldeperioder.last().antallDagerForPeriode shouldBe 2
        }
    }

    @Test
    fun `genererer meldeperioder for et stansvedtak`() {
        val fnr = Fnr.random()
        val sakId = SakId.random()
        val periode = Periode(2.januar(2023), 17.januar(2023))
        val innvilgelseVedtak = ObjectMother.nyRammevedtakInnvilgelse(fnr = fnr, sakId = sakId, periode = periode)
        val stansVedtak = ObjectMother.nyRammevedtakStans(fnr = fnr, sakId = sakId, periode = periode)
        val vedtaksliste = Vedtaksliste(listOf(innvilgelseVedtak, stansVedtak))

        val kjeder = MeldeperiodeKjeder(emptyList())

        val actual = kjeder.genererMeldeperioder(vedtaksliste, LocalDate.MAX)

        actual.let {
            it.first.size shouldBe 0
            it.second.size shouldBe 0
        }
    }

    @Test
    fun `genererer meldeperioder for innvilgelse, og deretter annulerer ved stans`() {
        val fnr = Fnr.random()
        val sakId = SakId.random()
        val periode = Periode(2.januar(2023), 17.januar(2023))
        val innvilgelseVedtak = ObjectMother.nyRammevedtakInnvilgelse(fnr = fnr, sakId = sakId, periode = periode)
        val v1 = Vedtaksliste(listOf(innvilgelseVedtak))
        val kjederV1 = MeldeperiodeKjeder(emptyList())

        val forventetFørstePeriode = Periode(2.januar(2023), 15.januar(2023))
        val forventetSistePeriode = Periode(16.januar(2023), 29.januar(2023))

        val (nyeKjederV1) = kjederV1.genererMeldeperioder(v1, LocalDate.MAX).also {
            it.first.meldeperioder.size shouldBe 2
            it.first.meldeperioder shouldBe it.second

            it.first.meldeperioder.first().periode shouldBe forventetFørstePeriode
            it.first.meldeperioder.last().periode shouldBe forventetSistePeriode

            it.first.meldeperioder.first().antallDagerSomGirRett shouldBe 14
            it.first.meldeperioder.first().antallDagerForPeriode shouldBe 10

            it.first.meldeperioder.last().antallDagerSomGirRett shouldBe 2
            it.first.meldeperioder.last().antallDagerForPeriode shouldBe 2
        }

        val stansVedtak = ObjectMother.nyRammevedtakStans(fnr = fnr, sakId = sakId, periode = periode)
        val v2 = Vedtaksliste(listOf(innvilgelseVedtak, stansVedtak))

        val actual = nyeKjederV1.genererMeldeperioder(v2, LocalDate.MAX)

        actual.let {
            it.first.meldeperioder.size shouldBe 2
            it.first.meldeperioder shouldBe it.second

            it.first.first().periode shouldBe forventetFørstePeriode
            it.first.last().periode shouldBe forventetSistePeriode

            it.first.meldeperioder.first().antallDagerSomGirRett shouldBe 0
            it.first.meldeperioder.first().antallDagerForPeriode shouldBe 0

            it.first.meldeperioder.last().antallDagerSomGirRett shouldBe 0
            it.first.meldeperioder.last().antallDagerForPeriode shouldBe 0
        }
    }
}
