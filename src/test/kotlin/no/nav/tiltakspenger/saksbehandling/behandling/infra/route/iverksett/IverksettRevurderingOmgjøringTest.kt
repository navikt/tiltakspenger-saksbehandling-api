package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
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
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import org.junit.jupiter.api.Test

class IverksettRevurderingOmgjøringTest {

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
                vedtaksperiode = omgjøringsperiode,
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
                vedtaksperiode = omgjøringsperiode,
                innvilgelsesperioder = innvilgelsesperioder(omgjøringsperiode),
            )

            // TODO jah: Fullfør etter vi har tilstrekkelig route-builder/verktøy i meldekort
        }
    }

    @Test
    fun `kan omgjøre et delvis omgjort vedtak`() {
        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val stansFraOgMed = 1.mars(2025)

            val omgjøringsperiode = 1.januar(2025) til 28.februar(2025)

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
                rammevedtakIdSomOmgjøres = søknadsbehandlingVedtak.id,
                vedtaksperiode = omgjøringsperiode,
                innvilgelsesperioder = innvilgelsesperioder(omgjøringsperiode),
            )

            sakMedOmgjøring.rammevedtaksliste.innvilgelsesperioder.perioder shouldBe listOf(omgjøringsperiode)

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
    fun `kan omgjøre en del midt i vedtaksperioden`() {
        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val omgjortPeriode = 1.februar(2025) til 28.februar(2025)

            val (sakMedSøknadsbehandling, _, søknadsbehandlingVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(førsteInnvilgelsesperiode),
            )

            val sakId = sakMedSøknadsbehandling.id

            val (sakMedOmgjøring, omgjøringsvedtak) = iverksettRevurderingOmgjøring(
                tac = tac,
                sakId = sakId,
                rammevedtakIdSomOmgjøres = søknadsbehandlingVedtak.id,
                innvilgelsesperioder = innvilgelsesperioder(omgjortPeriode),
                vedtaksperiode = omgjortPeriode,
            )

            omgjøringsvedtak.periode shouldBe omgjortPeriode

            sakMedOmgjøring.rammevedtaksliste.vedtaksperioder shouldBe listOf(
                1.januar(2025) til 31.januar(2025),
                1.februar(2025) til 28.februar(2025),
                1.mars(2025) til 31.mars(2025),
            )
        }
    }

    @Test
    fun `kan omgjøre en periode av et vedtak med to gjeldende perioder`() {
        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val revurdertPeriode = 1.februar(2025) til 28.februar(2025)
            val omgjortPeriode = 1.januar(2025) til 31.januar(2025)

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

            val (sakMedOmgjøring, omgjøringsvedtak) = iverksettRevurderingOmgjøring(
                tac = tac,
                sakId = sakId,
                rammevedtakIdSomOmgjøres = søknadsbehandlingVedtak.id,
                innvilgelsesperioder = innvilgelsesperioder(omgjortPeriode),
                vedtaksperiode = omgjortPeriode,
            )

            omgjøringsvedtak.periode shouldBe omgjortPeriode

            sakMedOmgjøring.rammevedtaksliste.vedtaksperioder shouldBe listOf(
                1.januar(2025) til 31.januar(2025),
                1.februar(2025) til 28.februar(2025),
                1.mars(2025) til 31.mars(2025),
            )
        }
    }

    @Test
    fun `kan omgjøre en delvis periode av et vedtak med to gjeldende perioder`() {
        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val revurdertPeriode = 1.februar(2025) til 28.februar(2025)
            val omgjortPeriode = 10.januar(2025) til 20.januar(2025)

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

            val (sakMedOmgjøring, omgjøringsvedtak) = iverksettRevurderingOmgjøring(
                tac = tac,
                sakId = sakId,
                rammevedtakIdSomOmgjøres = søknadsbehandlingVedtak.id,
                innvilgelsesperioder = innvilgelsesperioder(omgjortPeriode),
                vedtaksperiode = omgjortPeriode,
            )

            omgjøringsvedtak.periode shouldBe omgjortPeriode

            sakMedOmgjøring.rammevedtaksliste.vedtaksperioder shouldBe listOf(
                1.januar(2025) til 9.januar(2025),
                10.januar(2025) til 20.januar(2025),
                21.januar(2025) til 31.januar(2025),
                1.februar(2025) til 28.februar(2025),
                1.mars(2025) til 31.mars(2025),
            )
        }
    }
}
