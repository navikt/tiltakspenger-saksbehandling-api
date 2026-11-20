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
    fun `innvilgelseMedNøyaktigPeriode gir null når perioden bare delvis overlapper innvilgelsen`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1 til 31.januar(2025)
            val (sak, _, _, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, tiltaksdeltakelse(innvilgelsesperiode)),
            )
            val nyPeriode = 15.januar(2025) til 15.februar(2025)

            sak.rammevedtaksliste.innvilgelseMedNøyaktigPeriode(nyPeriode) shouldBe null
        }
    }

    /**
     * Ett vedtak med opphold i februar gir to innslag i den innvilgede tidslinja.
     * En søknad lik ett av innslagene er ikke lik hele perioden i vedtaket.
     * En søknad over hele spennet dekker oppholdet, som ikke er innvilget.
     * Begge gir null, og da skriver saksbehandler innvilgelsen i fritekst.
     */
    @Test
    fun `innvilgelseMedNøyaktigPeriode gir null for vedtak med opphold, både for én del og for hele spennet`() {
        withTestApplicationContext { tac ->
            val heleJanuar = 1 til 31.januar(2025)
            val heleMars = 1 til 31.mars(2025)
            val (sak, _, _, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(heleJanuar, heleMars),
            )

            sak.rammevedtaksliste.innvilgelseMedNøyaktigPeriode(heleMars) shouldBe null
            sak.rammevedtaksliste.innvilgelseMedNøyaktigPeriode(heleJanuar.fraOgMed til heleMars.tilOgMed) shouldBe null
        }
    }

    /**
     * Ulikt antall dager per meldeperiode gir to tilstøtende innvilgelsesperioder i samme vedtak.
     * De må slås sammen til ett innslag, ellers blir brevet aldri forhåndsutfylt for slike vedtak.
     * Hver del for seg er ikke lik hele innvilgelsen, og gir null.
     */
    @Test
    fun `innvilgelseMedNøyaktigPeriode slår sammen tilstøtende innvilgelsesperioder i samme vedtak`() {
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

            sak.rammevedtaksliste.innvilgelseMedNøyaktigPeriode(førstePeriode.fraOgMed til andrePeriode.tilOgMed) shouldBe
                PeriodeMedVerdi(innvilgelse, førstePeriode.fraOgMed til andrePeriode.tilOgMed)
            sak.rammevedtaksliste.innvilgelseMedNøyaktigPeriode(førstePeriode) shouldBe null
            sak.rammevedtaksliste.innvilgelseMedNøyaktigPeriode(andrePeriode) shouldBe null
        }
    }

    /**
     * En stans fra 16. januar gjør at bare 1. til 15. januar av innvilgelsen fortsatt gjelder.
     * Søknaden er lik den delen som gjelder, men ikke lik hele perioden i vedtaket.
     * Da skriver saksbehandler innvilgelsen i fritekst.
     */
    @Test
    fun `innvilgelseMedNøyaktigPeriode gir null når søknaden er lik den delen av et delvis stanset vedtak som gjelder`() {
        withTestApplicationContext { tac ->
            val heleJanuar = 1 til 31.januar(2025)
            val (sak, _, innvilgelse, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(heleJanuar),
            )

            val (sakEtterStans, _, _) = iverksettRevurderingStans(
                tac = tac,
                sakId = sak.id,
                stansFraOgMed = 16.januar(2025),
            )

            // Forutsetning: stansen har omgjort innvilgelsen fra 16. januar, ellers beviser testen ingenting.
            sakEtterStans.rammevedtaksliste.hentRammevedtakForId(innvilgelse.id)
                .omgjortAvRammevedtak.perioder shouldBe listOf(16.januar(2025) til 31.januar(2025))

            sakEtterStans.rammevedtaksliste.innvilgelseMedNøyaktigPeriode(1.januar(2025) til 15.januar(2025)) shouldBe null
            sakEtterStans.rammevedtaksliste.innvilgelseMedNøyaktigPeriode(heleJanuar) shouldBe null
        }
    }

    /**
     * To vedtak uten sammenheng, i januar og mars, gir hvert sitt innslag i den innvilgede tidslinja.
     * Søknaden er lik hele perioden i ett av vedtakene, og da forhåndsutfyller vi med det vedtaket.
     * De andre vedtakene i saken skal ikke hindre det.
     */
    @Test
    fun `innvilgelseMedNøyaktigPeriode gir vedtaket når søknaden er lik hele perioden i ett av flere vedtak`() {
        withTestApplicationContext { tac ->
            val heleJanuar = 1 til 31.januar(2025)
            val heleMars = 1 til 31.mars(2025)
            val (sak, _, januarvedtak, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(heleJanuar, tiltaksdeltakelse(heleJanuar)),
            )
            val (sakMedToVedtak, _, marsvedtak, _) = iverksettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                fnr = sak.fnr,
                innvilgelsesperioder = innvilgelsesperioder(heleMars, tiltaksdeltakelse(heleMars)),
            )

            sakMedToVedtak.rammevedtaksliste.innvilgelseMedNøyaktigPeriode(heleJanuar) shouldBe
                PeriodeMedVerdi(januarvedtak, heleJanuar)
            sakMedToVedtak.rammevedtaksliste.innvilgelseMedNøyaktigPeriode(heleMars) shouldBe
                PeriodeMedVerdi(marsvedtak, heleMars)
        }
    }

    /**
     * To tilstøtende vedtak gir sammen en sammenhengende innvilgelse fra januar til mars.
     * Søknaden dekker begge vedtakene og er ikke lik hele perioden i noen av dem.
     * Da skriver saksbehandler innvilgelsen i fritekst.
     */
    @Test
    fun `innvilgelseMedNøyaktigPeriode gir null når søknaden dekker to tilstøtende vedtak`() {
        withTestApplicationContext { tac ->
            val januarOgFebruar = 1.januar(2025) til 28.februar(2025)
            val heleMars = 1 til 31.mars(2025)
            val (sak, _, _, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(januarOgFebruar, tiltaksdeltakelse(januarOgFebruar)),
            )
            val (sakMedToVedtak, _, _, _) = iverksettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                fnr = sak.fnr,
                innvilgelsesperioder = innvilgelsesperioder(heleMars, tiltaksdeltakelse(heleMars)),
            )
            // Forutsetning: vedtakene gir to innslag uten hull, ellers beviser testen ingenting.
            sakMedToVedtak.rammevedtaksliste.innvilgetTidslinje.perioder shouldBe listOf(januarOgFebruar, heleMars)

            sakMedToVedtak.rammevedtaksliste.innvilgelseMedNøyaktigPeriode(januarOgFebruar.fraOgMed til heleMars.tilOgMed) shouldBe null
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
    fun `innvilgelseMedNøyaktigPeriode før avslaget ser bort fra revurdering etter avslaget`() {
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

            sakEtterStans.rammevedtaksliste.opprettetFør(avslag.opprettet).innvilgelseMedNøyaktigPeriode(avslag.periode) shouldBe
                PeriodeMedVerdi(innvilgelse, innvilgelsesperiode)
        }
    }
}
