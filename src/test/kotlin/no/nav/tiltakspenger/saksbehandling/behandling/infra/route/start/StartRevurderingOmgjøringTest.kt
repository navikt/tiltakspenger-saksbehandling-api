package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start

import arrow.core.nonEmptyListOf
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperiode
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class StartRevurderingOmgjøringTest {
    @Test
    fun `kan omgjøre stans`() {
        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val stansFraOgMedDato = 1.februar(2025)
            val (sak, _, _, rammevedtakRevurdering, _) = iverksettSøknadsbehandlingOgRevurderingStans(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(førsteInnvilgelsesperiode),
                stansFraOgMed = stansFraOgMedDato,
            )
            iverksettRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = rammevedtakRevurdering.id,
                innvilgelsesperioder = innvilgelsesperioder(1.februar(2025) til 31.mars(2025)),
            )
        }
    }

    @Test
    fun `kan omgjøre selv om det er hull i meldeperiodene`() {
        // 1. Lag sak med innvilget periode februar 2025
        // 2. Send inn et meldekort for februar (vi kunne sendt inn alle og, skal ikke spille noen rolle)
        // 3. Omgjør forrige vedtak og utvid til og med januar-febuar. (merk at det her ikke blir hull)
        // 4. Send inn det første meldekortet i januar.
        // 5. Omgjør vedtaket igjen, nå med hull i meldeperiodene.

        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.februar(2025) til 28.februar(2025)
            val omgjøringsperiode = 1.januar(2025) til 28.februar(2025)
            val (sak, _, rammevedtakSøknadsbehandling, _) = iverksettSøknadsbehandlingOgMeldekortbehandling(
                tac = tac,
                vedtaksperiode = førsteInnvilgelsesperiode,
                tiltaksdeltakelse = tiltaksdeltakelse(
                    fom = omgjøringsperiode.fraOgMed,
                    tom = omgjøringsperiode.tilOgMed,
                ),
            )!!
            val (_, vedtakOmgjøring1) = iverksettRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = rammevedtakSøknadsbehandling.id,
                innvilgelsesperioder = innvilgelsesperioder(omgjøringsperiode),
            )
            opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = MeldeperiodeKjedeId.fraPeriode(30.desember(2024) til 12.januar(2025)),
            )
            iverksettRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = vedtakOmgjøring1.id,
                innvilgelsesperioder = innvilgelsesperioder(omgjøringsperiode),
            )

            // TODO jah: Fullfør etter vi har tilstrekkelig route-builder/verktøy i meldekort
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
    fun `kan omgjøre et delvis omgjort vedtak`() {
        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val stansFraOgMed = 1.mars(2025)

            val omgjøringInnvilgelsesperioder = innvilgelsesperioder(1.januar(2025) til 28.februar(2025))

            val (sakMedSøknadsbehandling, _, søknadsbehandlingVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(førsteInnvilgelsesperiode),
            )

            val sakId = sakMedSøknadsbehandling.id

            iverksettRevurderingStans(
                tac = tac,
                sakId = sakId,
                stansFraOgMed = stansFraOgMed,
            )

            val (sakMedOmgjøring, omgjøringsvedtak) = iverksettRevurderingOmgjøring(
                tac = tac,
                sakId = sakId,
                innvilgelsesperioder = omgjøringInnvilgelsesperioder,
                rammevedtakIdSomOmgjøres = søknadsbehandlingVedtak.id,
            )

            sakMedOmgjøring.rammevedtaksliste.innvilgelsesperioder.perioder shouldBe omgjøringInnvilgelsesperioder.perioder

            omgjøringsvedtak.omgjørRammevedtak shouldBe OmgjørRammevedtak(
                Omgjøringsperiode(
                    rammevedtakId = søknadsbehandlingVedtak.id,
                    periode = 1.januar(2025) til 28.februar(2025),
                    omgjøringsgrad = Omgjøringsgrad.DELVIS,
                ),
            )
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
                vedtaksperiode = revurdertPeriode,
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
    fun `kan omgjøre deler av vedtaksperioden`() {
        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val omgjortPeriode = 1.februar(2025) til 28.februar(2025)

            val innvilgelsesperioder = innvilgelsesperioder(førsteInnvilgelsesperiode)

            val (sakMedSøknadsbehandling, _, søknadsbehandlingVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder,
            )

            val sakId = sakMedSøknadsbehandling.id

            val (_, nyOmgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sakId,
                rammevedtakIdSomOmgjøres = søknadsbehandlingVedtak.id,
            )!!

            val (_, oppdatertOmgjøring) = oppdaterRevurderingOmgjøring(
                tac = tac,
                sakId = sakId,
                behandlingId = nyOmgjøring.id,
                innvilgelsesperioder = innvilgelsesperioder(omgjortPeriode),
                vedtaksperiode = omgjortPeriode,
            )

            oppdatertOmgjøring.vedtaksperiode shouldBe omgjortPeriode
        }
    }

    @Test
    fun `kan omgjøre deler av vedtaksperioden 2`() {
        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val omgjortPeriode = 1.februar(2025) til 28.februar(2025)

            val innvilgelsesperioder = innvilgelsesperioder(førsteInnvilgelsesperiode)

            val (sakMedSøknadsbehandling, _, søknadsbehandlingVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder,
            )

            val sakId = sakMedSøknadsbehandling.id

            val (_, nyOmgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sakId,
                rammevedtakIdSomOmgjøres = søknadsbehandlingVedtak.id,
            )!!

            val (_, oppdatertOmgjøring) = oppdaterRevurderingOmgjøring(
                tac = tac,
                sakId = sakId,
                behandlingId = nyOmgjøring.id,
                innvilgelsesperioder = innvilgelsesperioder(omgjortPeriode),
                vedtaksperiode = omgjortPeriode,
            )

            oppdatertOmgjøring.vedtaksperiode shouldBe omgjortPeriode
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
}
