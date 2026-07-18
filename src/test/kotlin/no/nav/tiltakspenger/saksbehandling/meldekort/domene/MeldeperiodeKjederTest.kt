package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
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
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.GenererMeldeperioderFeil
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjede
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.finnNærmesteMeldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.genererMeldeperioderOgOppdaterKjeder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjortAvRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperiode
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldeperiodeKjederTest {

    @Nested
    inner class FinnNærmesteMeldeperiodeForDato {
        @Test
        fun `tom kjede finner start dato for en kjede`() {
            val tommeKjeder = MeldeperiodeKjeder(emptyList())
            tommeKjeder.finnNærmesteMeldeperiode(2.januar(2023)) shouldBe Periode(2.januar(2023), 15.januar(2023)).right()
            tommeKjeder.finnNærmesteMeldeperiode(7.april(2024)) shouldBe Periode(1.april(2024), 14.april(2024)).right()
            tommeKjeder.finnNærmesteMeldeperiode(8.april(2024)) shouldBe Periode(8.april(2024), 21.april(2024)).right()
            tommeKjeder.finnNærmesteMeldeperiode(9.april(2024)) shouldBe Periode(8.april(2024), 21.april(2024)).right()
            tommeKjeder.finnNærmesteMeldeperiode(10.april(2024)) shouldBe Periode(8.april(2024), 21.april(2024)).right()
            tommeKjeder.finnNærmesteMeldeperiode(11.april(2024)) shouldBe Periode(8.april(2024), 21.april(2024)).right()
            tommeKjeder.finnNærmesteMeldeperiode(12.april(2024)) shouldBe Periode(8.april(2024), 21.april(2024)).right()
            tommeKjeder.finnNærmesteMeldeperiode(13.april(2024)) shouldBe Periode(8.april(2024), 21.april(2024)).right()
            tommeKjeder.finnNærmesteMeldeperiode(14.april(2024)) shouldBe Periode(8.april(2024), 21.april(2024)).right()
            tommeKjeder.finnNærmesteMeldeperiode(15.april(2024)) shouldBe Periode(15.april(2024), 28.april(2024)).right()
        }

        @Test
        fun `angitt dato er 1 meldeperiode før kjedens meldeperiode`() {
            val kjeder = MeldeperiodeKjeder(
                MeldeperiodeKjede(ObjectMother.meldeperiode(periode = Periode(20.januar(2025), 2.februar(2025)))),
            )
            kjeder.finnNærmesteMeldeperiode(6.januar(2025)) shouldBe Periode(6.januar(2025), 19.januar(2025)).right()
            kjeder.finnNærmesteMeldeperiode(19.januar(2025)) shouldBe Periode(6.januar(2025), 19.januar(2025)).right()
        }

        @Test
        fun `angitt dato er 2 meldeperiode før kjedens meldeperiode`() {
            val kjeder = MeldeperiodeKjeder(
                MeldeperiodeKjede(ObjectMother.meldeperiode(periode = Periode(20.januar(2025), 2.februar(2025)))),
            )
            kjeder.finnNærmesteMeldeperiode(23.desember(2024)) shouldBe Periode(23.desember(2024), 5.januar(2025)).right()
            kjeder.finnNærmesteMeldeperiode(5.januar(2025)) shouldBe Periode(23.desember(2024), 5.januar(2025)).right()
        }

        @Test
        fun `angitt dato er etter meldeperioden`() {
            val kjeder = MeldeperiodeKjeder(
                MeldeperiodeKjede(ObjectMother.meldeperiode(periode = Periode(6.januar(2025), 19.januar(2025)))),
            )
            kjeder.finnNærmesteMeldeperiode(25.januar(2025)) shouldBe Periode(
                20.januar(2025),
                2.februar(2025),
            ).right()
        }

        @Test
        fun `angitt dato er 1 meldeperiode etter kjedens-meldeperioden`() {
            val kjeder = MeldeperiodeKjeder(
                MeldeperiodeKjede(ObjectMother.meldeperiode(periode = Periode(20.januar(2025), 2.februar(2025)))),
            )
            kjeder.finnNærmesteMeldeperiode(3.februar(2025)) shouldBe Periode(3.februar(2025), 16.februar(2025)).right()
            kjeder.finnNærmesteMeldeperiode(16.februar(2025)) shouldBe Periode(3.februar(2025), 16.februar(2025)).right()
        }

        @Test
        fun `angitt dato er 2 meldeperiode etter kjedens-meldeperioden`() {
            val kjeder = MeldeperiodeKjeder(
                MeldeperiodeKjede(ObjectMother.meldeperiode(periode = Periode(20.januar(2025), 2.februar(2025)))),
            )
            kjeder.finnNærmesteMeldeperiode(17.februar(2025)) shouldBe Periode(17.februar(2025), 2.mars(2025)).right()
            kjeder.finnNærmesteMeldeperiode(2.mars(2025)) shouldBe Periode(17.februar(2025), 2.mars(2025)).right()
        }

        @Test
        fun `angitt dato er innenfor meldeperioden`() {
            val kjeder = MeldeperiodeKjeder(
                MeldeperiodeKjede(ObjectMother.meldeperiode(periode = Periode(6.januar(2025), 19.januar(2025)))),
            )
            kjeder.finnNærmesteMeldeperiode(6.januar(2025)) shouldBe Periode(6.januar(2025), 19.januar(2025)).right()
            kjeder.finnNærmesteMeldeperiode(19.januar(2025)) shouldBe Periode(6.januar(2025), 19.januar(2025)).right()
        }

        @Test
        fun `LocalDate MIN og MAX gir UgyldigDato`() {
            val tommeKjeder = MeldeperiodeKjeder(emptyList())
            tommeKjeder.finnNærmesteMeldeperiode(LocalDate.MIN) shouldBe GenererMeldeperioderFeil.UgyldigDato.left()
            tommeKjeder.finnNærmesteMeldeperiode(LocalDate.MAX) shouldBe GenererMeldeperioderFeil.UgyldigDato.left()

            val kjeder = MeldeperiodeKjeder(
                MeldeperiodeKjede(ObjectMother.meldeperiode(periode = Periode(6.januar(2025), 19.januar(2025)))),
            )
            kjeder.finnNærmesteMeldeperiode(LocalDate.MIN) shouldBe GenererMeldeperioderFeil.UgyldigDato.left()
            kjeder.finnNærmesteMeldeperiode(LocalDate.MAX) shouldBe GenererMeldeperioderFeil.UgyldigDato.left()
        }
    }

    @Test
    fun `genererer meldeperioder for et innvilgelse vedtak`() {
        val sakId = SakId.random()
        val periode = Periode(2.januar(2023), 17.januar(2023))
        val kjeder = MeldeperiodeKjeder(emptyList())
        val innvilgelseVedtak = ObjectMother.nyRammevedtakInnvilgelse(
            sakId = sakId,
            innvilgelsesperioder = listOf(
                innvilgelsesperiodeKommando(innvilgelsesperiode = periode),
            ),
        )
        val actual = kjeder.genererMeldeperioderOgOppdaterKjeder(
            Rammevedtaksliste(
                innvilgelseVedtak,
            ),
            fixedClock,
        ).getOrNull()!!

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
        val innvilgelseVedtak =
            ObjectMother.nyRammevedtakInnvilgelse(
                fnr = fnr,
                sakId = sakId,
                innvilgelsesperioder = listOf(
                    innvilgelsesperiodeKommando(innvilgelsesperiode = periode),
                ),
            )
        val stansVedtak = ObjectMother.nyRammevedtakStans(
            fnr = fnr,
            sakId = sakId,
            periode = periode,
            opprettet = nå(enUkeEtterFixedClock),
            omgjørRammevedtak = OmgjørRammevedtak(
                Omgjøringsperiode(
                    rammevedtakId = innvilgelseVedtak.id,
                    periode = periode,
                    omgjøringsgrad = Omgjøringsgrad.HELT,
                ),
            ),
        )
        val vedtaksliste = Rammevedtaksliste(
            listOf(
                innvilgelseVedtak.copy(
                    omgjortAvRammevedtak = OmgjortAvRammevedtak(
                        Omgjøringsperiode(
                            rammevedtakId = stansVedtak.id,
                            periode = periode,
                            omgjøringsgrad = Omgjøringsgrad.HELT,
                        ),
                    ),
                ),
                stansVedtak,
            ),
        )

        val kjeder = MeldeperiodeKjeder(emptyList())

        val actual = kjeder.genererMeldeperioderOgOppdaterKjeder(vedtaksliste, fixedClock).getOrNull()!!

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
        val innvilgelseVedtak =
            ObjectMother.nyRammevedtakInnvilgelse(
                fnr = fnr,
                sakId = sakId,
                innvilgelsesperioder = listOf(
                    innvilgelsesperiodeKommando(
                        innvilgelsesperiode = periode,
                    ),
                ),
            )
        val v1 = Rammevedtaksliste(listOf(innvilgelseVedtak))
        val kjederV1 = MeldeperiodeKjeder(emptyList())

        val forventetFørstePeriode = Periode(2.januar(2023), 15.januar(2023))
        val forventetSistePeriode = Periode(16.januar(2023), 29.januar(2023))

        val (nyeKjederV1) = kjederV1.genererMeldeperioderOgOppdaterKjeder(v1, fixedClock).getOrNull()!!.also {
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
            omgjørRammevedtak = OmgjørRammevedtak(
                Omgjøringsperiode(
                    rammevedtakId = innvilgelseVedtak.id,
                    periode = periode,
                    omgjøringsgrad = Omgjøringsgrad.HELT,
                ),
            ),
        )
        val v2 = Rammevedtaksliste(
            listOf(
                innvilgelseVedtak.copy(
                    omgjortAvRammevedtak = OmgjortAvRammevedtak(
                        Omgjøringsperiode(
                            rammevedtakId = stansVedtak.id,
                            periode = periode,
                            omgjøringsgrad = Omgjøringsgrad.HELT,
                        ),
                    ),
                ),
                stansVedtak,
            ),
        )

        val actual = nyeKjederV1.genererMeldeperioderOgOppdaterKjeder(v2, enUkeEtterFixedClock).getOrNull()!!

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
        val clock = TikkendeKlokke()
        val saksnummer = Saksnummer.genererSaknummer(løpenr = "2001", clock = clock)
        val fnr = Fnr.random()
        val periode1 = 1 til 14.september(2025)
        val periode2 = 15 til 28.september(2025)
        val meldeperiode1V1 = ObjectMother.meldeperiode(
            clock = clock,
            periode = periode1,
            saksnummer = saksnummer,
            versjon = HendelseVersjon(1),
            sakId = sakId,
            fnr = fnr,
        )
        val meldeperiode1V2 = ObjectMother.meldeperiode(
            clock = clock,
            periode = periode1,
            saksnummer = saksnummer,
            versjon = HendelseVersjon(2),
            fnr = fnr,
            sakId = sakId,
            antallDagerForPeriode = 9,
        )
        val meldeperiode2 = ObjectMother.meldeperiode(
            clock = clock,
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
        kjeder.hentForegåendeMeldeperiodekjede(meldeperiode2.kjedeId) shouldBe MeldeperiodeKjede(
            meldeperiode1V1,
            meldeperiode1V2,
        )
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
            innvilgelsesperioder = listOf(
                innvilgelsesperiodeKommando(
                    innvilgelsesperiode = periode,
                    antallDagerPerMeldeperiode = 5,
                ),
            ),
        )
        val actual = kjeder.genererMeldeperioderOgOppdaterKjeder(
            Rammevedtaksliste(
                innvilgelseVedtak,
            ),
            fixedClock,
        ).getOrNull()!!

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

    @Test
    fun `tom vedtaksliste og tomme kjeder gir tomt resultat`() {
        val kjeder = MeldeperiodeKjeder(emptyList())

        val actual = kjeder.genererMeldeperioderOgOppdaterKjeder(Rammevedtaksliste(emptyList()), fixedClock).getOrNull()!!

        actual.first shouldBe kjeder
        actual.second shouldBe emptyList()
    }

    @Test
    fun `tom vedtaksliste men eksisterende kjeder gir feil`() {
        val kjeder = MeldeperiodeKjeder(
            MeldeperiodeKjede(ObjectMother.meldeperiode(periode = Periode(2.januar(2023), 15.januar(2023)))),
        )

        kjeder.genererMeldeperioderOgOppdaterKjeder(Rammevedtaksliste(emptyList()), fixedClock) shouldBe
            GenererMeldeperioderFeil.HarMeldeperioderUtenVedtaksperioder.left()
    }

    @Test
    fun `kjøring med uendret vedtaksliste lager ingen nye versjoner (idempotent)`() {
        val sakId = SakId.random()
        val periode = Periode(2.januar(2023), 17.januar(2023))
        val innvilgelseVedtak = ObjectMother.nyRammevedtakInnvilgelse(
            sakId = sakId,
            innvilgelsesperioder = listOf(
                innvilgelsesperiodeKommando(innvilgelsesperiode = periode),
            ),
        )
        val vedtaksliste = Rammevedtaksliste(innvilgelseVedtak)

        val (kjederEtterFørste, nyeFørste) =
            MeldeperiodeKjeder(emptyList()).genererMeldeperioderOgOppdaterKjeder(vedtaksliste, fixedClock).getOrNull()!!

        nyeFørste.size shouldBe 2
        kjederEtterFørste.sisteMeldeperiodePerKjede.forEach { it.versjon shouldBe HendelseVersjon(1) }

        val (kjederEtterAndre, nyeAndre) =
            kjederEtterFørste.genererMeldeperioderOgOppdaterKjeder(vedtaksliste, fixedClock).getOrNull()!!

        // Ingen endringer -> ingen nye meldeperioder og kjedene er uendret
        nyeAndre shouldBe emptyList()
        kjederEtterAndre shouldBe kjederEtterFørste
        kjederEtterAndre.sisteMeldeperiodePerKjede.forEach { it.versjon shouldBe HendelseVersjon(1) }
    }

    @Test
    fun `maks antall dager kappes når innvilgelsesperioden bare delvis dekker meldeperioden`() {
        val sakId = SakId.random()
        // 2. januar 2023 er en mandag.
        // Innvilger kun mandag og tirsdag, altså 2 dager med rett i en meldeperiode som strekker seg 2. - 15. januar.
        val innvilgelsesperiode = Periode(2.januar(2023), 3.januar(2023))
        val kjeder = MeldeperiodeKjeder(emptyList())
        val innvilgelseVedtak = ObjectMother.nyRammevedtakInnvilgelse(
            sakId = sakId,
            innvilgelsesperioder = listOf(
                innvilgelsesperiodeKommando(
                    innvilgelsesperiode = innvilgelsesperiode,
                    antallDagerPerMeldeperiode = 10,
                ),
            ),
        )

        val actual =
            kjeder.genererMeldeperioderOgOppdaterKjeder(Rammevedtaksliste(innvilgelseVedtak), fixedClock).getOrNull()!!

        actual.let {
            it.first.size shouldBe 1
            it.first.sisteMeldeperiodePerKjede shouldBe it.second

            val meldeperiode = it.first.sisteMeldeperiodePerKjede.single()
            meldeperiode.periode shouldBe Periode(2.januar(2023), 15.januar(2023))
            meldeperiode.antallDagerSomGirRett shouldBe 2
            // Selv om vedtaket tillater 10 dager, kappes maks til antall dager som faktisk gir rett.
            meldeperiode.maksAntallDagerForMeldeperiode shouldBe 2
        }
    }
}
