package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

import arrow.core.nonEmptyListOf
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSaksopplysningerForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class OppdaterRevurderingOmgjøringRouteTest {

    @Test
    fun `revurdering til omgjøring - kan oppdatere behandlingen etter saksopplysninger har endret seg`() {
        withTestApplicationContext { tac ->
            // Omgjøringen starter med at tiltaksdeltakelsesperioden er endret siden søknadsvedtaket.
            val (sak, _, rammevedtakSøknadsbehandling, rammevedtakRevurdering) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(
                tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder((1.april(2025) til 10.april(2025))),
            )!!

            val tiltaksdeltakelseVedOpprettelseAvRevurdering =
                rammevedtakRevurdering.saksopplysninger.tiltaksdeltakelser.first()
            val nyOmgjøringsperiodeEtterOppdatering = (3 til 9.april(2025))
            val avbruttTiltaksdeltakelse = tiltaksdeltakelseVedOpprettelseAvRevurdering.copy(
                deltakelseFraOgMed = tiltaksdeltakelseVedOpprettelseAvRevurdering.deltakelseFraOgMed!!,
                deltakelseTilOgMed = tiltaksdeltakelseVedOpprettelseAvRevurdering.deltakelseTilOgMed!!.minusDays(1),
                deltakelseStatus = TiltakDeltakerstatus.Avbrutt,
            )
            // Under behandlingen endrer tiltaksdeltakelsen seg igjen.
            tac.oppdaterTiltaksdeltakelse(fnr = sak.fnr, tiltaksdeltakelse = avbruttTiltaksdeltakelse)
            val (_, revurderingMedOppdatertSaksopplysninger: Rammebehandling) = oppdaterSaksopplysningerForBehandlingId(
                tac,
                sak.id,
                rammevedtakRevurdering.id,
            )
            (revurderingMedOppdatertSaksopplysninger as Revurdering).erFerdigutfylt() shouldBe true
            val barnetillegg = Barnetillegg.utenBarnetillegg((3 til 9.april(2025)))
            val innvilgelsesperioder = innvilgelsesperioder(
                nyOmgjøringsperiodeEtterOppdatering,
                tiltaksdeltakelseVedOpprettelseAvRevurdering,
            )

            val (_, oppdatertRevurdering) = oppdaterRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammevedtakRevurdering.id,
                fritekstTilVedtaksbrev = "asdf",
                begrunnelseVilkårsvurdering = null,
                innvilgelsesperioder = innvilgelsesperioder,
                barnetillegg = barnetillegg,
                forventetStatus = HttpStatusCode.OK,
            )
            (oppdatertRevurdering as Revurdering).erFerdigutfylt() shouldBe true
            // Forventer at saksopplysningene er oppdatert, mens resultatet er ubesudlet.
            oppdatertRevurdering.saksopplysninger.tiltaksdeltakelser.single() shouldBe avbruttTiltaksdeltakelse
            val resultat = oppdatertRevurdering.resultat as Revurderingsresultat.Omgjøring
            // Kommentar jah: Beklager for alt todomain-greiene. Her bør det expectes på eksplisitte verdier uten å bruke domenekode for mapping.
            resultat.barnetillegg shouldBe barnetillegg
            resultat.antallDagerPerMeldeperiode shouldBe innvilgelsesperioder.antallDagerPerMeldeperiode
            resultat.valgteTiltaksdeltakelser shouldBe listOf(
                PeriodeMedVerdi(
                    avbruttTiltaksdeltakelse,
                    nyOmgjøringsperiodeEtterOppdatering,
                ),
            ).tilIkkeTomPeriodisering()
            oppdatertRevurdering.vedtaksperiode shouldBe rammevedtakSøknadsbehandling.rammebehandling.vedtaksperiode
            resultat.vedtaksperiode shouldBe rammevedtakSøknadsbehandling.rammebehandling.vedtaksperiode
            resultat.innvilgelsesperioder!!.totalPeriode shouldBe nyOmgjøringsperiodeEtterOppdatering
            oppdatertRevurdering.utbetaling shouldBe null

            // Forsikrer oss om at vi ikke har brutt noen init-regler i Sak.kt.
            tac.sakContext.sakService.hentForSakId(sakId = rammevedtakRevurdering.sakId).rammebehandlinger[1] shouldBe oppdatertRevurdering
        }
    }

    @Test
    fun `kan ikke innvilge utenfor valgt omgjøringsperiode innenfor et vedtak`() {
        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val omgjortPeriode = 1.februar(2025) til 28.februar(2025)

            val innvilgelsesperioder = innvilgelsesperioder(førsteInnvilgelsesperiode)

            val (sakMedSøknadsbehandling, _, søknadsbehandlingVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder,
            )

            val sakId = sakMedSøknadsbehandling.id

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sakId,
                rammevedtakIdSomOmgjøres = søknadsbehandlingVedtak.id,
            )!!

            oppdaterRevurderingOmgjøring(
                tac = tac,
                sakId = sakId,
                innvilgelsesperioder = innvilgelsesperioder(omgjortPeriode.plusTilOgMed(1)),
                behandlingId = omgjøring.id,
                omgjøringsperiode = omgjortPeriode,
                forventetStatus = HttpStatusCode.BadRequest,
            ).also {
                it.second.omgjørRammevedtak.rammevedtakIDer.single() shouldBe søknadsbehandlingVedtak.id

                @Language("JSON")
                val expectedResponse = """
                    {
                        "melding": "Innvilgelsesperiodene kan ikke overlappe med de deler av gjeldende vedtaksperioder som ikke omgjøres",
                        "kode":"innvilgelsesperioder_overlapper_ikkeomgjort_periode"
                    }
                """.trimIndent()

                it.third.shouldEqualJson(expectedResponse)
            }
        }
    }

    @Test
    fun `kan ikke omgjøre over flere vedtak`() {
        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val stansFraOgMed = 1.mars(2025)

            val innvilgelsesperioder = innvilgelsesperioder(førsteInnvilgelsesperiode)

            val (sakMedSøknadsbehandling, _, søknadsbehandlingVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder,
            )

            val sakId = sakMedSøknadsbehandling.id

            iverksettRevurderingStans(
                tac = tac,
                sakId = sakId,
                stansFraOgMed = stansFraOgMed,
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sakId,
                rammevedtakIdSomOmgjøres = søknadsbehandlingVedtak.id,
            )!!

            oppdaterRevurderingOmgjøring(
                tac = tac,
                sakId = sakId,
                innvilgelsesperioder = innvilgelsesperioder,
                behandlingId = omgjøring.id,
                forventetStatus = HttpStatusCode.BadRequest,
            ).also {
                it.second.omgjørRammevedtak.rammevedtakIDer.single() shouldBe søknadsbehandlingVedtak.id

                @Language("JSON")
                val expectedResponse = """
                    {
                        "melding": "En omgjøring kan kun omgjøre ett tidligere vedtak",
                        "kode":"kan_ikke_omgjøre_flere_vedtak"
                    }
                """.trimIndent()

                it.third.shouldEqualJson(expectedResponse)
            }
        }
    }

    @Test
    fun `kan ikke omgjøre over hull i vedtaksperioden til et vedtak`() {
        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val revurdertPeriode = 1.februar(2025) til 28.februar(2025)

            val innvilgelsesperioder = innvilgelsesperioder(førsteInnvilgelsesperiode)

            val (sakMedSøknadsbehandling, _, søknadsbehandlingVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder,
            )

            val sakId = sakMedSøknadsbehandling.id

            iverksettRevurderingInnvilgelse(
                tac = tac,
                sakId = sakId,
                innvilgelsesperioder = innvilgelsesperioder(
                    revurdertPeriode,
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(2),
                ),
            )

            val (_, nyOmgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sakId,
                rammevedtakIdSomOmgjøres = søknadsbehandlingVedtak.id,
            )!!

            val (_, _, responseBody) = oppdaterRevurderingOmgjøring(
                tac = tac,
                sakId = sakId,
                behandlingId = nyOmgjøring.id,
                innvilgelsesperioder = innvilgelsesperioder(revurdertPeriode),
                omgjøringsperiode = revurdertPeriode,
                forventetStatus = HttpStatusCode.BadRequest,
            )

            responseBody.shouldEqualJson(
                """
                {
                  "melding": "Må omgjøre angitt vedtak",
                  "kode": "må_omgjøre_angitt_vedtak"
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `kan revurdere med hull`() {
        /**
         * 1. Søknadsbehandling: innvilger januar 2025
         * 2. Søknadsbehandling: innvilger mars 2025
         * 3. Meldekort: beregner/utbetaler alle meldekort
         * 4. Revurdering: innvilger jan-mars 2025
         */
        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.januar(2025) til 31.januar(2025)
            val andreInnvilgelsesperiode = 1.mars(2025) til 31.mars(2025)
            val saksbehandler = saksbehandler()
            val tiltaksdeltakelse = tiltaksdeltakelse(
                fom = førsteInnvilgelsesperiode.fraOgMed,
                tom = andreInnvilgelsesperiode.tilOgMed,
            )
            val (sakMedFørsteSøknadsbehandling, _, _, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(førsteInnvilgelsesperiode, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )
            val sakId = sakMedFørsteSøknadsbehandling.id
            val (sakMedAndreSøknadsbehandling, _, _, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(andreInnvilgelsesperiode, tiltaksdeltakelse),
                sakId = sakId,
                tiltaksdeltakelse = tiltaksdeltakelse,
            )
            sakMedAndreSøknadsbehandling.meldeperiodeKjeder.forEach {
                opprettOgIverksettMeldekortbehandling(
                    tac = tac,
                    sakId = sakId,
                    kjedeId = it.kjedeId,
                )
            }
            val (_, opprettetRevurdering, _) = startRevurderingInnvilgelse(
                tac = tac,
                sakId = sakId,
            )!!

            val (_, behandlingMedHull) = oppdaterRevurderingInnvilgelse(
                tac = tac,
                sakId = sakId,
                behandlingId = opprettetRevurdering.id,
                saksbehandler = saksbehandler,
                fritekstTilVedtaksbrev = "fritekstTilVedtaksbrev",
                begrunnelseVilkårsvurdering = "begrunnelseVilkårsvurdering",
                innvilgelsesperioder = innvilgelsesperioder(
                    innvilgelsesperiode(
                        periode = førsteInnvilgelsesperiode,
                        valgtTiltaksdeltakelse = tiltaksdeltakelse,
                    ),
                    innvilgelsesperiode(
                        periode = andreInnvilgelsesperiode,
                        valgtTiltaksdeltakelse = tiltaksdeltakelse,
                    ),
                ),
            )

            behandlingMedHull.innvilgelsesperioder!!.periodisering.erSammenhengende shouldBe false

            behandlingMedHull.innvilgelsesperioder!!.perioder shouldBe nonEmptyListOf(
                førsteInnvilgelsesperiode,
                andreInnvilgelsesperiode,
            )
        }
    }

    @Test
    fun `kan revurdere over hull`() {
        /**
         * 1. Søknadsbehandling: innvilger januar 2025
         * 2. Søknadsbehandling: innvilger mars 2025
         * 3. Meldekort: beregner/utbetaler alle meldekort
         * 4. Revurdering: innvilger jan-mars 2025
         */
        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.januar(2025) til 31.januar(2025)
            val andreInnvilgelsesperiode = 1.mars(2025) til 31.mars(2025)
            val revurderingsperiode = 1.januar(2025) til 31.mars(2025)
            val saksbehandler = saksbehandler()
            val tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
                fom = revurderingsperiode.fraOgMed,
                tom = revurderingsperiode.tilOgMed,
            )
            val (sakMedFørsteSøknadsbehandling, _, _, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(førsteInnvilgelsesperiode, tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )
            val sakId = sakMedFørsteSøknadsbehandling.id
            val (sakMedAndreSøknadsbehandling, _, _, _) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(andreInnvilgelsesperiode, tiltaksdeltakelse),
                sakId = sakId,
                tiltaksdeltakelse = tiltaksdeltakelse,
            )
            sakMedAndreSøknadsbehandling.meldeperiodeKjeder.forEach {
                opprettOgIverksettMeldekortbehandling(
                    tac = tac,
                    sakId = sakId,
                    kjedeId = it.kjedeId,
                )
            }
            val (_, opprettetRevurdering, _) = startRevurderingInnvilgelse(
                tac = tac,
                sakId = sakId,
            )!!
            oppdaterRevurderingInnvilgelse(
                tac = tac,
                sakId = sakId,
                behandlingId = opprettetRevurdering.id,
                saksbehandler = saksbehandler,
                fritekstTilVedtaksbrev = "fritekstTilVedtaksbrev",
                begrunnelseVilkårsvurdering = "begrunnelseVilkårsvurdering",
                innvilgelsesperioder = innvilgelsesperioder(
                    periode = revurderingsperiode,
                    valgtTiltaksdeltakelse = tiltaksdeltakelse,
                ),
            )
        }
    }
}
