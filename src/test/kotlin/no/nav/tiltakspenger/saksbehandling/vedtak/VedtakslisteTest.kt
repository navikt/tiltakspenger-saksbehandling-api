package no.nav.tiltakspenger.saksbehandling.vedtak

import arrow.core.nonEmptySetOf
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.dato.september
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingAvslag
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
    fun `gjeldendeInnvilgelseForPeriode gir hele den innvilgede perioden, ikke bare overlappen`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1 til 31.januar(2025)
            val (sak, _, innvilgelse, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, tiltaksdeltakelse(innvilgelsesperiode)),
            )
            val nyPeriode = 15.januar(2025) til 15.februar(2025)

            sak.rammevedtaksliste.gjeldendeInnvilgelseForPeriode(nyPeriode).also { gjeldendeInnvilgelse ->
                gjeldendeInnvilgelse shouldBe PeriodeMedVerdi(innvilgelse, innvilgelsesperiode)
            }
        }
    }

    /**
     * Ett vedtak med hull mellom januar og mars gir to innslag i den innvilgede tidslinja.
     * Bare innslagene som overlapper den nye perioden skal telle, og overlapper begge, får saksbehandler skrive innvilgelsen i fritekst (avklart med fag).
     */
    @Test
    fun `gjeldendeInnvilgelseForPeriode velger delen som overlapper, og gir null når flere deler overlapper`() {
        withTestApplicationContext { tac ->
            val heleJanuar = 1 til 31.januar(2025)
            val heleMars = 1 til 31.mars(2025)
            val (sak, _, innvilgelse, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(heleJanuar, heleMars),
            )

            sak.rammevedtaksliste.gjeldendeInnvilgelseForPeriode(heleMars) shouldBe PeriodeMedVerdi(innvilgelse, heleMars)
            sak.rammevedtaksliste.gjeldendeInnvilgelseForPeriode(heleJanuar.fraOgMed til heleMars.tilOgMed) shouldBe null
        }
    }

    /**
     * Ulikt antall dager per meldeperiode gir to tilstøtende innvilgelsesperioder i samme vedtak.
     * De må slås sammen til ett innslag, ellers faller brevet tilbake til reserveteksten for helt vanlige vedtak.
     */
    @Test
    fun `gjeldendeInnvilgelseForPeriode slår sammen tilstøtende innvilgelsesperioder i samme vedtak`() {
        withTestApplicationContext { tac ->
            val førstePeriode = 1 til 14.september(2025)
            val andrePeriode = 15 til 28.september(2025)
            val tiltaksdeltakelse = tiltaksdeltakelse(fom = førstePeriode.fraOgMed, tom = andrePeriode.tilOgMed)
            val (sak, _, innvilgelse, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(
                    innvilgelsesperiode(
                        periode = førstePeriode,
                        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(9),
                        valgtTiltaksdeltakelse = tiltaksdeltakelse,
                    ),
                    innvilgelsesperiode(
                        periode = andrePeriode,
                        antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(8),
                        valgtTiltaksdeltakelse = tiltaksdeltakelse,
                    ),
                ),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )
            // Forutsetning: vedtaket har to innvilgelsesperioder, ellers beviser testen ingenting.
            innvilgelse.innvilgelsesperioder!!.perioder shouldBe listOf(førstePeriode, andrePeriode)

            sak.rammevedtaksliste.gjeldendeInnvilgelseForPeriode(førstePeriode) shouldBe
                PeriodeMedVerdi(innvilgelse, førstePeriode.fraOgMed til andrePeriode.tilOgMed)
        }
    }

    @Test
    fun `opprettetFør tar bort senere vedtak og regner omgjøringene på nytt`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1 til 31.januar(2025)
            val (sak, _, innvilgelse, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, tiltaksdeltakelse(innvilgelsesperiode)),
            )
            // Stansen omgjør innvilgelsen fra og med 16. januar.
            val (sakEtterStans, stans, _) = iverksettRevurderingStans(
                tac = tac,
                sakId = sak.id,
                stansFraOgMed = 16.januar(2025),
            )

            // Forutsetning: stansen har omgjort innvilgelsen fra 16. januar, ellers beviser testen ingenting.
            sakEtterStans.rammevedtaksliste.hentRammevedtakForId(innvilgelse.id)
                .omgjortAvRammevedtak.perioder shouldBe listOf(16.januar(2025) til 31.januar(2025))

            sakEtterStans.rammevedtaksliste.opprettetFør(stans.opprettet).also { førStansen ->
                førStansen shouldContainExactly listOf(innvilgelse)
                førStansen.hentRammevedtakForId(innvilgelse.id).omgjortAvRammevedtak.perioder shouldBe emptyList()
                førStansen.innvilgetTidslinje.perioder shouldBe listOf(innvilgelsesperiode)
            }
        }
    }

    @Test
    fun `gjeldendeInnvilgelseForPeriode før avslaget ser bort fra revurdering etter avslaget`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1 til 31.januar(2025)
            val tiltaksdeltakelse = tiltaksdeltakelse(innvilgelsesperiode)
            val (sak, _, innvilgelse, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, tiltaksdeltakelse),
            )
            val (_, avslag) = iverksettSøknadsbehandlingAvslag(
                tac = tac,
                sakId = sak.id,
                fnr = sak.fnr,
                tiltaksdeltakelse = tiltaksdeltakelse,
                avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.InnvilgetTiltakspenger),
            )
            // Stansen kommer etter avslaget og skal ikke påvirke hva som gjaldt da avslaget ble fattet.
            val (sakEtterStans, _, _) = iverksettRevurderingStans(
                tac = tac,
                sakId = sak.id,
                stansFraOgMed = 16.januar(2025),
            )

            sakEtterStans.rammevedtaksliste.opprettetFør(avslag.opprettet).gjeldendeInnvilgelseForPeriode(avslag.periode).also { gjeldendeInnvilgelse ->
                gjeldendeInnvilgelse shouldBe PeriodeMedVerdi(innvilgelse, innvilgelsesperiode)
            }
        }
    }
}
