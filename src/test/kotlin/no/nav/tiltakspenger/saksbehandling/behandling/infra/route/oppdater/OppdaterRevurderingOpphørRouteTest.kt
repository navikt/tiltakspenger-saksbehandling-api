package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringIkkeValgt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringOpphør
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import org.junit.jupiter.api.Test

class OppdaterRevurderingOpphørRouteTest {

    @Test
    fun `skal oppdatere revurdering med opphør av hele vedtaket`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)

            val (sak, _, søknadVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadVedtak.id,
                nyOmgjøring = true,
            )!!

            val (_, oppdatertOmgjøring) = oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = innvilgelsesperiode,
            )

            oppdatertOmgjøring.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringOpphør>()
            oppdatertOmgjøring.resultat!!.vedtaksperiode shouldBe innvilgelsesperiode
        }
    }

    @Test
    fun `skal oppdatere revurdering med opphør av deler av vedtaket`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val opphørsperiode = 15.februar(2025) til 31.mars(2025)

            val (sak, _, søknadVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadVedtak.id,
                nyOmgjøring = true,
            )!!

            val (_, oppdatertOmgjøring) = oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = opphørsperiode,
            )

            oppdatertOmgjøring.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringOpphør>()
            oppdatertOmgjøring.resultat!!.vedtaksperiode shouldBe opphørsperiode
            oppdatertOmgjøring.resultat!!.omgjørRammevedtak.perioder.single() shouldBe opphørsperiode
            oppdatertOmgjøring.resultat!!.omgjørRammevedtak.rammevedtakIDer.single() shouldBe søknadVedtak.id
        }
    }

    @Test
    fun `skal oppdatere revurdering med opphør og så tilbake til ikke valgt resultat`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val opphørsperiode = 15.februar(2025) til 31.mars(2025)

            val (sak, _, søknadVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadVedtak.id,
                nyOmgjøring = true,
            )!!

            oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = opphørsperiode,
            )

            val (_, oppdatertOmgjøring) = oppdaterOmgjøringIkkeValgt(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
            )

            oppdatertOmgjøring.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringIkkeValgt>()
            oppdatertOmgjøring.resultat!!.vedtaksperiode shouldBe null
            oppdatertOmgjøring.resultat!!.omgjørRammevedtak.rammevedtakIDer.single() shouldBe søknadVedtak.id
            oppdatertOmgjøring.resultat!!.omgjørRammevedtak.perioder.single() shouldBe innvilgelsesperiode
        }
    }
}
