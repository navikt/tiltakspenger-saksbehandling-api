package no.nav.tiltakspenger.saksbehandling.vedtak

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.september
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import org.junit.jupiter.api.Test

class VedtakslisteTest {
    @Test
    fun `default antall dager for hele innvilgelsesperioden`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1 til 31.januar(2025)
            val tiltaksdeltakelse = tiltaksdeltakelse(innvilgelsesperiode)
            val (sak, _, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                resultat = SøknadsbehandlingsresultatType.INNVILGELSE,
                tiltaksdeltakelse = tiltaksdeltakelse,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, tiltaksdeltakelse(innvilgelsesperiode)),
            )
            sak.rammevedtaksliste.antallDagerPerMeldeperiode shouldBe Periodisering(
                AntallDagerForMeldeperiode.default,
                innvilgelsesperiode,
            )
            sak.rammevedtaksliste.antallDagerForMeldeperiode(16.desember(2024) til 29.desember(2024)) shouldBe null
            sak.rammevedtaksliste.antallDagerForMeldeperiode(30.desember(2024) til 12.januar(2025)) shouldBe AntallDagerForMeldeperiode.default
            sak.rammevedtaksliste.antallDagerForMeldeperiode(13.januar(2025) til 26.januar(2025)) shouldBe AntallDagerForMeldeperiode.default
            sak.rammevedtaksliste.antallDagerForMeldeperiode(27.januar(2025) til 9.februar(2025)) shouldBe AntallDagerForMeldeperiode.default
            sak.rammevedtaksliste.antallDagerForMeldeperiode(10.februar(2025) til 23.februar(2025)) shouldBe null
        }
    }

    @Test
    fun `ren splitt 2 meldeperioder`() {
        withTestApplicationContext { tac ->
            // 2 hele meldeperioder
            val innvilgelsesperiode = 1 til 28.september(2025)

            val tiltaksdeltakelse =
                tiltaksdeltakelse(fom = innvilgelsesperiode.fraOgMed, tom = innvilgelsesperiode.tilOgMed)

            val innvilgelsesperioder = innvilgelsesperioder(
                innvilgelsesperiode(
                    periode = 1 til 14.september(2025),
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(9),
                    valgtTiltaksdeltakelse = tiltaksdeltakelse,
                ),
                innvilgelsesperiode(
                    periode = 15 til 28.september(2025),
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(8),
                    valgtTiltaksdeltakelse = tiltaksdeltakelse,
                ),
            )
            val (sak, _, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                resultat = SøknadsbehandlingsresultatType.INNVILGELSE,
                innvilgelsesperioder = innvilgelsesperioder,
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            sak.rammevedtaksliste.innvilgelsesperioder.verdier shouldBe innvilgelsesperioder.periodisering.verdier
            sak.rammevedtaksliste.antallDagerForMeldeperiode(1 til 14.september(2025)) shouldBe AntallDagerForMeldeperiode(
                9,
            )
            sak.rammevedtaksliste.antallDagerForMeldeperiode(15 til 28.september(2025)) shouldBe AntallDagerForMeldeperiode(
                8,
            )
        }
    }

    @Test
    fun `kutter andre meldeperiode i 2`() {
        withTestApplicationContext { tac ->
            // 2 hele meldeperioder
            val innvilgelsesperiode = 1 til 28.september(2025)

            val tiltaksdeltakelse =
                tiltaksdeltakelse(fom = innvilgelsesperiode.fraOgMed, tom = innvilgelsesperiode.tilOgMed)

            val innvilgelsesperioder = innvilgelsesperioder(
                innvilgelsesperiode(
                    periode = 1 til 21.september(2025),
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(9),
                    valgtTiltaksdeltakelse = tiltaksdeltakelse,
                ),
                innvilgelsesperiode(
                    periode = 22 til 28.september(2025),
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(8),
                    valgtTiltaksdeltakelse = tiltaksdeltakelse,
                ),
            )
            val (sak, _, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                resultat = SøknadsbehandlingsresultatType.INNVILGELSE,
                innvilgelsesperioder = innvilgelsesperioder,
                tiltaksdeltakelse = tiltaksdeltakelse,
            )
            sak.rammevedtaksliste.innvilgelsesperioder.verdier shouldBe innvilgelsesperioder.periodisering.verdier
            sak.rammevedtaksliste.antallDagerForMeldeperiode(1 til 14.september(2025)) shouldBe AntallDagerForMeldeperiode(
                9,
            )
            sak.rammevedtaksliste.antallDagerForMeldeperiode(15 til 28.september(2025)) shouldBe AntallDagerForMeldeperiode(
                9,
            )
        }
    }

    @Test
    fun `1 dag inn i neste meldeperiode`() {
        withTestApplicationContext { tac ->
            // 2 hele meldeperioder
            val innvilgelsesperiode = 1 til 28.september(2025)

            val tiltaksdeltakelse =
                tiltaksdeltakelse(fom = innvilgelsesperiode.fraOgMed, tom = innvilgelsesperiode.tilOgMed)

            val innvilgelsesperioder = innvilgelsesperioder(
                innvilgelsesperiode(
                    periode = 1 til 15.september(2025),
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(9),
                    valgtTiltaksdeltakelse = tiltaksdeltakelse,
                ),
                innvilgelsesperiode(
                    periode = 16 til 28.september(2025),
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(8),
                    valgtTiltaksdeltakelse = tiltaksdeltakelse,
                ),
            )
            val (sak, _, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                resultat = SøknadsbehandlingsresultatType.INNVILGELSE,
                innvilgelsesperioder = innvilgelsesperioder,
                tiltaksdeltakelse = tiltaksdeltakelse,
            )
            sak.rammevedtaksliste.innvilgelsesperioder.verdier shouldBe innvilgelsesperioder.periodisering.verdier
            sak.rammevedtaksliste.antallDagerForMeldeperiode(1 til 14.september(2025)) shouldBe AntallDagerForMeldeperiode(
                9,
            )
            sak.rammevedtaksliste.antallDagerForMeldeperiode(15 til 28.september(2025)) shouldBe AntallDagerForMeldeperiode(
                9,
            )
        }
    }

    @Test
    fun `gjeldendeInnvilgelseForPeriode gir null når saken ikke har innvilgelser`() {
        // TODO: Tom Rammevedtaksliste, eller en sak med bare et avslag.
        // Forvent null.
        TODO("Ikke implementert")
    }

    @Test
    fun `gjeldendeInnvilgelseForPeriode gir null når innvilgelsen ikke overlapper perioden`() {
        // TODO: Innvilgelse i januar, spør om en periode i mars.
        // Forvent null.
        TODO("Ikke implementert")
    }

    @Test
    fun `gjeldendeInnvilgelseForPeriode gir hele den innvilgede perioden, ikke bare overlappen`() {
        // TODO: Innvilgelse 1.–31. januar, spør om 15. januar–15. februar.
        // Forvent vedtaket, med periode 1.–31. januar.
        TODO("Ikke implementert")
    }

    @Test
    fun `gjeldendeInnvilgelseForPeriode gir null når flere innvilgelser overlapper perioden`() {
        // TODO: To innvilgelser, f.eks. januar og februar, spør om en periode som dekker begge.
        // Forvent null, så saksbehandler skriver periodene i fritekst.
        TODO("Ikke implementert")
    }

    @Test
    fun `gjeldendeInnvilgelseForPeriode gir null når ett vedtak har innvilgelsesperioder med hull som begge overlapper`() {
        // TODO: Ett vedtak med to innvilgelsesperioder med hull mellom, f.eks. jan og mars, og avslagsperiode jan–mars.
        // Forvent null, slik at saksbehandler fyller inn innvilgelsen manuelt i brevet (avklart med fag).
        TODO("Ikke implementert")
    }

    @Test
    fun `gjeldendeInnvilgelseForPeriode slår sammen tilstøtende innvilgelsesperioder i samme vedtak`() {
        // TODO: Ett vedtak med to tilstøtende innvilgelsesperioder, f.eks. ulikt antall dager per meldeperiode i jan og feb.
        // Forvent én PeriodeMedVerdi med hele perioden jan–feb, siden tilPeriodisering slår sammen like verdier.
        TODO("Ikke implementert")
    }

    @Test
    fun `gjeldendeInnvilgelseForPeriode gir den gjeldende delen når innvilgelsen er delvis stanset`() {
        // TODO: Innvilgelse 1.–31. januar, så stans fra 16. januar.
        // Forvent vedtaket med periode 1.–15. januar.
        TODO("Ikke implementert")
    }

    @Test
    fun `opprettetFør gir tom liste når ingen vedtak er eldre enn tidspunktet`() {
        // TODO: Iverksett én innvilgelse.
        // Kall opprettetFør med vedtakets egen opprettet-tidspunkt.
        // Forvent tom liste, siden vedtak opprettet på tidspunktet ikke tas med.
        TODO("Ikke implementert")
    }

    @Test
    fun `opprettetFør tar bort senere vedtak og regner omgjøringene på nytt`() {
        // TODO: Iverksett en innvilgelse, deretter en revurdering som omgjør deler av den.
        // Kall opprettetFør med revurderingens opprettet-tidspunkt.
        // Forvent at lista bare har innvilgelsen, og at omgjortAvRammevedtak på den er tom.
        // Forvent at innvilgetTidslinje igjen dekker hele den opprinnelige innvilgelsesperioden.
        TODO("Ikke implementert")
    }

    @Test
    fun `gjeldendeInnvilgelseForPeriode før avslaget ser bort fra revurdering etter avslaget`() {
        // TODO: Innvilgelse, så avslag med grunn InnvilgetTiltakspenger, så revurdering som endrer innvilgelsen.
        // Kall opprettetFør(avslaget.opprettet).gjeldendeInnvilgelseForPeriode(avslaget.periode).
        // Forvent den opprinnelige innvilgelsen med hele den opprinnelige perioden.
        TODO("Ikke implementert")
    }
}
