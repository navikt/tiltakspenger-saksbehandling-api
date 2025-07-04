package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.enUkeEtterFixedClock
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.dato.september
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

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
    fun `genererer meldeperioder for et innvilgelse vedtak`() {
        val sakId = SakId.random()
        val periode = Periode(2.januar(2023), 17.januar(2023))
        val kjeder = MeldeperiodeKjeder(emptyList())
        val innvilgelseVedtak = ObjectMother.nyRammevedtakInnvilgelse(sakId = sakId, periode = periode)
        val actual = kjeder.genererMeldeperioder(
            Vedtaksliste(
                innvilgelseVedtak,
            ),
            fixedClock,
        )

        val forventetFørstePeriode = Periode(2.januar(2023), 15.januar(2023))
        val forventetSistePeriode = Periode(16.januar(2023), 29.januar(2023))

        actual.let {
            it.first.sisteMeldeperiodePerKjede shouldBe it.second

            it.first.first().periode shouldBe forventetFørstePeriode
            it.first.last().periode shouldBe forventetSistePeriode

            it.first.sisteMeldeperiodePerKjede.first().antallDagerSomGirRett shouldBe 14
            it.first.sisteMeldeperiodePerKjede.first().maksAntallDagerForMeldeperiode shouldBe 10

            it.first.sisteMeldeperiodePerKjede.last().antallDagerSomGirRett shouldBe 2
            it.first.sisteMeldeperiodePerKjede.last().maksAntallDagerForMeldeperiode shouldBe 2
        }
    }

    @Test
    fun `genererer meldeperioder for et stansvedtak`() {
        val fnr = Fnr.random()
        val sakId = SakId.random()
        val periode = Periode(2.januar(2023), 17.januar(2023))
        val innvilgelseVedtak = ObjectMother.nyRammevedtakInnvilgelse(fnr = fnr, sakId = sakId, periode = periode)
        val stansVedtak = ObjectMother.nyRammevedtakStans(
            fnr = fnr,
            sakId = sakId,
            periode = periode,
            opprettet = nå(
                enUkeEtterFixedClock,
            ),
        )
        val vedtaksliste = Vedtaksliste(
            listOf(
                innvilgelseVedtak,
                stansVedtak,
            ),
        )

        val kjeder = MeldeperiodeKjeder(emptyList())

        val actual = kjeder.genererMeldeperioder(vedtaksliste, fixedClock)

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

        val (nyeKjederV1) = kjederV1.genererMeldeperioder(v1, fixedClock).also {
            it.first.sisteMeldeperiodePerKjede.size shouldBe 2
            it.first.sisteMeldeperiodePerKjede shouldBe it.second

            it.first.sisteMeldeperiodePerKjede.first().periode shouldBe forventetFørstePeriode
            it.first.sisteMeldeperiodePerKjede.last().periode shouldBe forventetSistePeriode

            it.first.sisteMeldeperiodePerKjede.first().antallDagerSomGirRett shouldBe 14
            it.first.sisteMeldeperiodePerKjede.first().maksAntallDagerForMeldeperiode shouldBe 10

            it.first.sisteMeldeperiodePerKjede.last().antallDagerSomGirRett shouldBe 2
            it.first.sisteMeldeperiodePerKjede.last().maksAntallDagerForMeldeperiode shouldBe 2
        }

        val stansVedtak = ObjectMother.nyRammevedtakStans(
            fnr = fnr,
            sakId = sakId,
            periode = periode,
            opprettet = nå(
                enUkeEtterFixedClock,
            ),
        )
        val v2 = Vedtaksliste(
            listOf(
                innvilgelseVedtak,
                stansVedtak,
            ),
        )

        val actual = nyeKjederV1.genererMeldeperioder(v2, enUkeEtterFixedClock)

        actual.let {
            it.first.sisteMeldeperiodePerKjede.size shouldBe 2
            it.first.sisteMeldeperiodePerKjede shouldBe it.second

            it.first.first().periode shouldBe forventetFørstePeriode
            it.first.last().periode shouldBe forventetSistePeriode

            it.first.sisteMeldeperiodePerKjede.first().antallDagerSomGirRett shouldBe 0
            it.first.sisteMeldeperiodePerKjede.first().maksAntallDagerForMeldeperiode shouldBe 0

            it.first.sisteMeldeperiodePerKjede.last().antallDagerSomGirRett shouldBe 0
            it.first.sisteMeldeperiodePerKjede.last().maksAntallDagerForMeldeperiode shouldBe 0
        }
    }

    @Test
    fun `test forskjellige hent-funksjoner`() {
        val sakId = SakId.random()
        val saksnummer = Saksnummer.genererSaknummer(løpenr = "2001")
        val fnr = Fnr.random()
        val periode1 = 1 til 14.september(2025)
        val periode2 = 15 til 28.september(2025)
        val meldeperiode1V1 = ObjectMother.meldeperiode(
            periode = periode1,
            saksnummer = saksnummer,
            versjon = HendelseVersjon(1),
            sakId = sakId,
            fnr = fnr,
        )
        val meldeperiode1V2 = ObjectMother.meldeperiode(
            periode = periode1,
            saksnummer = saksnummer,
            versjon = HendelseVersjon(2),
            fnr = fnr,
            sakId = sakId,
            antallDagerForPeriode = 9,
        )
        val meldeperiode2 = ObjectMother.meldeperiode(
            periode = periode2,
            saksnummer = saksnummer,
            sakId = sakId,
            fnr = fnr,
        )
        val k1 = MeldeperiodeKjede(meldeperiode1V1, meldeperiode1V2)
        val k2 = MeldeperiodeKjede(meldeperiode2)
        val kjeder = MeldeperiodeKjeder(k1, k2)
        kjeder.hentMeldeperioderForPeriode(periode1) shouldBe listOf(meldeperiode1V2)
        kjeder.hentMeldeperioderForPeriode(periode2) shouldBe listOf(meldeperiode2)
        kjeder.hentMeldeperioderForPeriode(14 til 15.september(2025)) shouldBe listOf(meldeperiode1V2, meldeperiode2)
        kjeder.hentMeldeperiode(14 til 15.september(2025)) shouldBe null
        kjeder.hentMeldeperiode(periode1) shouldBe meldeperiode1V2
        kjeder.hentMeldeperiode(periode2) shouldBe meldeperiode2
        kjeder.hentForMeldeperiodeId(meldeperiode1V1.id) shouldBe meldeperiode1V1
        kjeder.hentForMeldeperiodeId(meldeperiode1V2.id) shouldBe meldeperiode1V2
        kjeder.hentForMeldeperiodeId(meldeperiode2.id) shouldBe meldeperiode2
        kjeder.hentSisteMeldeperiodeForKjede(meldeperiode1V1.kjedeId) shouldBe meldeperiode1V2
        kjeder.hentSisteMeldeperiodeForKjede(meldeperiode1V2.kjedeId) shouldBe meldeperiode1V2
        kjeder.hentSisteMeldeperiodeForKjede(meldeperiode2.kjedeId) shouldBe meldeperiode2
        kjeder.hentMeldeperiodeKjedeForPeriode(periode1) shouldBe MeldeperiodeKjede(meldeperiode1V1, meldeperiode1V2)
        kjeder.hentMeldeperiodeKjedeForPeriode(periode2) shouldBe MeldeperiodeKjede(meldeperiode2)
        kjeder.hentMeldeperiodeKjedeForPeriode(14 til 15.september(2025)) shouldBe null
        kjeder.hentForegåendeMeldeperiodekjede(meldeperiode1V1.kjedeId) shouldBe null
        kjeder.hentForegåendeMeldeperiodekjede(meldeperiode2.kjedeId) shouldBe MeldeperiodeKjede(meldeperiode1V1, meldeperiode1V2)
        kjeder.hentSisteMeldeperiodeForKjedeId(meldeperiode1V1.kjedeId) shouldBe meldeperiode1V2
        kjeder.hentSisteMeldeperiodeForKjedeId(meldeperiode2.kjedeId) shouldBe meldeperiode2
    }

    @Test
    fun `genererer meldeperioder for et innvilgelsevedtak 5 dager pr meldeperiode`() {
        val sakId = SakId.random()
        val periode = Periode(2.januar(2023), 17.januar(2023))
        val kjeder = MeldeperiodeKjeder(emptyList())
        val innvilgelseVedtak = ObjectMother.nyRammevedtakInnvilgelse(
            sakId = sakId,
            periode = periode,
            antallDagerPerMeldeperiode = SammenhengendePeriodisering(
                AntallDagerForMeldeperiode(5),
                periode,
            ),
        )
        val actual = kjeder.genererMeldeperioder(
            Vedtaksliste(
                innvilgelseVedtak,
            ),
            fixedClock,
        )

        val forventetFørstePeriode = Periode(2.januar(2023), 15.januar(2023))
        val forventetSistePeriode = Periode(16.januar(2023), 29.januar(2023))

        actual.let {
            it.first.sisteMeldeperiodePerKjede.size shouldBe 2
            it.first.sisteMeldeperiodePerKjede shouldBe it.second

            it.first.first().periode shouldBe forventetFørstePeriode
            it.first.last().periode shouldBe forventetSistePeriode

            it.first.sisteMeldeperiodePerKjede.first().antallDagerSomGirRett shouldBe 14
            it.first.sisteMeldeperiodePerKjede.first().maksAntallDagerForMeldeperiode shouldBe 5

            it.first.sisteMeldeperiodePerKjede.last().antallDagerSomGirRett shouldBe 2
            it.first.sisteMeldeperiodePerKjede.last().maksAntallDagerForMeldeperiode shouldBe 2
        }
    }
}
