package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class StartRevurderingOmgjøringTest {
    @Disabled("TODO jah: Midlertidig deaktivert fordi omgjøring av stans ikke fungerer atm.")
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
}
