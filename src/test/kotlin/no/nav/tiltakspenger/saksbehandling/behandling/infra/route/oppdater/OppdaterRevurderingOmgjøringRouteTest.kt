package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

import arrow.core.nonEmptyListOf
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.utenBarnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Prioritering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat.OmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.harKode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperiode
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSaksopplysningerForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import org.junit.jupiter.api.Test

class OppdaterRevurderingOmgjøringRouteTest {

    @Test
    fun `revurdering til omgjøring - kan oppdatere behandlingen etter saksopplysninger har endret seg`() {
        withTestApplicationContext { tac ->
            // Omgjøringen starter med at tiltaksdeltakelsesperioden er endret siden søknadsvedtaket.
            val (sak, _, rammevedtakSøknadsbehandling, nyOmgjøring) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(
                tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder((1.april(2025) til 10.april(2025))),
            )!!

            val tiltaksdeltakelseVedOpprettelseAvRevurdering =
                nyOmgjøring.saksopplysninger.tiltaksdeltakelser.first()
            val nyInnvilgelsesperiode = (3 til 9.april(2025))
            val avbruttTiltaksdeltakelse = tiltaksdeltakelseVedOpprettelseAvRevurdering.copy(
                deltakelseFraOgMed = tiltaksdeltakelseVedOpprettelseAvRevurdering.deltakelseFraOgMed!!,
                deltakelseTilOgMed = tiltaksdeltakelseVedOpprettelseAvRevurdering.deltakelseTilOgMed!!.minusDays(1),
                deltakelseStatus = TiltakDeltakerstatus.Avbrutt,
            )

            // Under behandlingen endrer tiltaksdeltakelsen seg igjen.
            tac.oppdaterTiltaksdeltakelse(fnr = sak.fnr, tiltaksdeltakelse = avbruttTiltaksdeltakelse)
            oppdaterSaksopplysningerForBehandlingId(
                tac,
                sak.id,
                nyOmgjøring.id,
            )

            val barnetillegg = Barnetillegg.utenBarnetillegg((3 til 9.april(2025)))
            val innvilgelsesperioder = innvilgelsesperioder(
                nyInnvilgelsesperiode,
                tiltaksdeltakelseVedOpprettelseAvRevurdering,
            )

            val (_, oppdatertRevurdering) = oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = nyOmgjøring.id,
                fritekstTilVedtaksbrev = "asdf",
                begrunnelseVilkårsvurdering = null,
                vedtaksperiode = rammevedtakSøknadsbehandling.periode,
                innvilgelsesperioder = innvilgelsesperioder,
                barnetillegg = barnetillegg,
                forventet = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
            )

            (oppdatertRevurdering as Revurdering).erFerdigutfylt() shouldBe true
            // Forventer at saksopplysningene er oppdatert, mens resultatet er ubesudlet.
            oppdatertRevurdering.saksopplysninger.tiltaksdeltakelser.single() shouldBe avbruttTiltaksdeltakelse
            val resultat = oppdatertRevurdering.resultat as OmgjøringInnvilgelse
            // Kommentar jah: Beklager for alt todomain-greiene.
            // Her bør det expectes på eksplisitte verdier uten å bruke domenekode for mapping.
            resultat.barnetillegg shouldBe barnetillegg
            resultat.antallDagerPerMeldeperiode shouldBe innvilgelsesperioder.antallDagerPerMeldeperiode
            resultat.valgteTiltaksdeltakelser shouldBe listOf(
                PeriodeMedVerdi(
                    avbruttTiltaksdeltakelse,
                    nyInnvilgelsesperiode,
                ),
            ).tilIkkeTomPeriodisering()
            oppdatertRevurdering.vedtaksperiode shouldBe rammevedtakSøknadsbehandling.rammebehandling.vedtaksperiode
            resultat.vedtaksperiode shouldBe rammevedtakSøknadsbehandling.rammebehandling.vedtaksperiode
            resultat.innvilgelsesperioder!!.totalPeriode shouldBe nyInnvilgelsesperiode
            oppdatertRevurdering.utbetaling shouldBe null

            // Forsikrer oss om at vi ikke har brutt noen init-regler i Sak.kt.
            tac.sakContext.sakService.hentForSakId(sakId = nyOmgjøring.sakId).rammebehandlinger[1] shouldBe oppdatertRevurdering
        }
    }

    @Test
    fun `kan ikke innvilge utenfor valgt vedtaksperioder for et vedtak`() {
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

            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sakId,
                innvilgelsesperioder = innvilgelsesperioder(omgjortPeriode.plusTilOgMed(1)),
                behandlingId = omgjøring.id,
                vedtaksperiode = omgjortPeriode,
                // Vil feile på [Sak.beregnRammebehandling].
                // Vi sjekker også i oppdater-funksjonen for revurdering omgjøring.
                forventet = ForventetRespons(500, contentType = "application/json; charset=UTF-8"),
            ).also {
                it.second.omgjørRammevedtak.rammevedtakIDer.single() shouldBe søknadsbehandlingVedtak.id
            }
        }
    }

    @Test
    fun `kan oppdatere uten brev`() {
        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.januar(2025) til 31.mars(2025)

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

            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sakId,
                innvilgelsesperioder = innvilgelsesperioder,
                behandlingId = omgjøring.id,
                vedtaksperiode = førsteInnvilgelsesperiode,
                skalSendeVedtaksbrev = false,
                prioriteringBegrunnelse = "Haster",
            ).also {
                it.second.skalSendeVedtaksbrev shouldBe false
                it.second.prioritering shouldBe Prioritering.create("Haster")
            }
        }
    }

    @Test
    fun `kan omgjøre over flere vedtak`() {
        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val stansFraOgMed = 1.mars(2025)

            val innvilgelsesperioder = innvilgelsesperioder(førsteInnvilgelsesperiode)

            val (sakMedSøknadsbehandling, _, søknadsbehandlingVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder,
            )

            val sakId = sakMedSøknadsbehandling.id

            val (_, stansvedtak) = iverksettRevurderingStans(
                tac = tac,
                sakId = sakId,
                stansFraOgMed = stansFraOgMed,
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sakId,
                rammevedtakIdSomOmgjøres = søknadsbehandlingVedtak.id,
            )!!

            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sakId,
                vedtaksperiode = førsteInnvilgelsesperiode,
                innvilgelsesperioder = innvilgelsesperioder,
                behandlingId = omgjøring.id,
            ).also {
                it.second.omgjørRammevedtak shouldBe OmgjørRammevedtak(
                    Omgjøringsperiode(
                        rammevedtakId = søknadsbehandlingVedtak.id,
                        periode = 1.januar(2025) til 28.februar(2025),
                        omgjøringsgrad = Omgjøringsgrad.DELVIS,
                    ),
                    Omgjøringsperiode(
                        rammevedtakId = stansvedtak.id,
                        periode = 1.mars(2025) til 31.mars(2025),
                        omgjøringsgrad = Omgjøringsgrad.HELT,
                    ),
                )

                it.third.shouldEqualSpecifiedJson(
                    """
                    {
                      "omgjørVedtak": "${søknadsbehandlingVedtak.id}",
                      "omgjørRammevedtak": [
                        {
                          "vedtakId": "${søknadsbehandlingVedtak.id}",
                          "periode": {"fraOgMed": "2025-01-01", "tilOgMed": "2025-02-28"},
                          "omgjøringsgrad": "DELVIS"
                        },
                        {
                          "vedtakId": "${stansvedtak.id}",
                          "periode": {"fraOgMed": "2025-03-01", "tilOgMed": "2025-03-31"},
                          "omgjøringsgrad": "HELT"
                        }
                      ]
                    }
                    """.trimIndent(),
                )
            }
        }
    }

    @Test
    fun `omgjør vedtaket som gjelder for vedtaksperioden, ikke vedtaket omgjøringen ble startet fra`() {
        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val revurdertPeriode = 1.februar(2025) til 28.februar(2025)

            val innvilgelsesperioder = innvilgelsesperioder(førsteInnvilgelsesperiode)

            val (sakMedSøknadsbehandling, _, søknadsbehandlingVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder,
            )

            val sakId = sakMedSøknadsbehandling.id

            val (_, revurderingsvedtak) = iverksettRevurderingInnvilgelse(
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

            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sakId,
                behandlingId = nyOmgjøring.id,
                innvilgelsesperioder = innvilgelsesperioder(revurdertPeriode),
                vedtaksperiode = revurdertPeriode,
            ).also {
                it.second.omgjørRammevedtak shouldBe OmgjørRammevedtak(
                    Omgjøringsperiode(
                        rammevedtakId = revurderingsvedtak.id,
                        periode = revurdertPeriode,
                        omgjøringsgrad = Omgjøringsgrad.HELT,
                    ),
                )
            }
        }
    }

    @Test
    fun `kan omgjøre over flere vedtak med hull mellom seg`() {
        withTestApplicationContext { tac ->
            val tiltaksdeltakelse = tiltaksdeltakelse(fom = 1.januar(2025), tom = 31.mars(2025))
            val (sak, _, januarvedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1.januar(2025) til 31.januar(2025), tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )
            val (_, _, marsvedtak) = iverksettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                innvilgelsesperioder = innvilgelsesperioder(1.mars(2025) til 31.mars(2025), tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = januarvedtak.id,
            )!!

            val vedtaksperiode = 1.januar(2025) til 31.mars(2025)
            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = vedtaksperiode,
                innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode, tiltaksdeltakelse),
            ).also {
                it.second.vedtaksperiode shouldBe vedtaksperiode
                it.second.omgjørRammevedtak shouldBe OmgjørRammevedtak(
                    Omgjøringsperiode(
                        rammevedtakId = januarvedtak.id,
                        periode = 1.januar(2025) til 31.januar(2025),
                        omgjøringsgrad = Omgjøringsgrad.HELT,
                    ),
                    Omgjøringsperiode(
                        rammevedtakId = marsvedtak.id,
                        periode = 1.mars(2025) til 31.mars(2025),
                        omgjøringsgrad = Omgjøringsgrad.HELT,
                    ),
                )
            }
        }
    }

    @Test
    fun `kan ikke opphøre deler av vedtaksperioden som ikke har tidligere vedtak`() {
        withTestApplicationContext { tac ->
            val tiltaksdeltakelse = tiltaksdeltakelse(fom = 1.januar(2025), tom = 31.mars(2025))
            val (sak, _, januarvedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1.januar(2025) til 31.januar(2025), tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )
            iverksettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                innvilgelsesperioder = innvilgelsesperioder(1.mars(2025) til 31.mars(2025), tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )
            val (_, omgjøring) = startRevurderingOmgjøring(tac, sak.id, januarvedtak.id)!!

            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = 1.januar(2025) til 31.mars(2025),
                innvilgelsesperioder = innvilgelsesperioder(
                    innvilgelsesperiode(periode = 1.januar(2025) til 31.januar(2025), valgtTiltaksdeltakelse = tiltaksdeltakelse),
                    innvilgelsesperiode(periode = 15.februar(2025) til 31.mars(2025), valgtTiltaksdeltakelse = tiltaksdeltakelse),
                ),
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ).also { (_, _, response) ->
                response harKode "ugyldig_periode_for_opphør"
                response shouldContain "Perioder som opphøres kan ikke gå over perioder uten tidligere vedtak: 1.–14. februar 2025"
            }
        }
    }

    @Test
    fun `kan ikke omgjøre med innvilgelse over en periode uten tidligere vedtak`() {
        withTestApplicationContext { tac ->
            val tiltaksdeltakelse = tiltaksdeltakelse(fom = 1.januar(2025), tom = 31.mars(2025))
            val (sak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1.januar(2025) til 31.januar(2025), tiltaksdeltakelse),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )
            val (_, omgjøring) = startRevurderingOmgjøring(tac, sak.id, rammevedtakIdSomOmgjøres = null)!!

            val vedtaksperiode = 1.mars(2025) til 31.mars(2025)
            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = vedtaksperiode,
                innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode, tiltaksdeltakelse),
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ).also { (_, _, response) ->
                response harKode "må_omgjøre_minst_ett_vedtak"
            }
        }
    }

    @Test
    fun `kan revurdere med hull`() {
        /*
         1. Søknadsbehandling: innvilger januar 2025
         2. Søknadsbehandling: innvilger mars 2025
         3. Meldekort: beregner/utbetaler alle meldekort
         4. Revurdering: innvilger jan-mars 2025
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
        /*
         1. Søknadsbehandling: innvilger januar 2025
         2. Søknadsbehandling: innvilger mars 2025
         3. Meldekort: beregner/utbetaler alle meldekort
         4. Revurdering: innvilger jan-mars 2025
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
