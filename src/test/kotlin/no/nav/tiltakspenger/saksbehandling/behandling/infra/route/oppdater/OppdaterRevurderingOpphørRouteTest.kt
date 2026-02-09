package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.harKode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
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

    @Test
    fun `skal ikke kunne opphøre perioder som ikke er gjeldende for vedtaket`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val stansetPeriode = 15.februar(2025) til 31.mars(2025)

            val (sak, _, søknadVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            iverksettRevurderingStans(
                tac = tac,
                sakId = sak.id,
                stansFraOgMed = stansetPeriode.fraOgMed,
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadVedtak.id,
            )!!

            oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = innvilgelsesperiode,
                forventetStatus = HttpStatusCode.BadRequest,
            ).also { (_, _, response) ->
                response harKode "kan_ikke_omgjøre_flere_vedtak"
            }

            oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = stansetPeriode,
                forventetStatus = HttpStatusCode.BadRequest,
            ).also { (_, _, response) ->
                response harKode "må_omgjøre_angitt_vedtak"
            }
        }
    }

    @Test
    fun `skal ikke kunne opphøre et vedtak uten gjeldende innvilgelse`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val stansetPeriode = 1.mars(2025) til 31.mars(2025)

            val (sak, stansvedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            ).let {
                iverksettRevurderingStans(
                    tac = tac,
                    sakId = it.first.id,
                    stansFraOgMed = stansetPeriode.fraOgMed,
                )
            }

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = stansvedtak.id,
            )!!

            oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = stansetPeriode,
                forventetStatus = HttpStatusCode.BadRequest,
            ).also { (_, _, response) ->
                response harKode "vedtak_kan_ikke_opphøres_uten_gjeldende_innvilgelse"
            }
        }
    }

    @Test
    fun `skal ikke kunne opphøre en periode uten gjeldende innvilgelse`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperioder = arrayOf(1.januar(2025) til 31.januar(2025), 1.mars(2025) til 31.mars(2025))

            val (sak, _, søknadvedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(*innvilgelsesperioder),
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadvedtak.id,
            )!!

            oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = 1.februar(2025) til 28.februar(2025),
                forventetStatus = HttpStatusCode.BadRequest,
            ).also { (_, _, response) ->
                response harKode "ugyldig_periode_for_opphør"
            }

            oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = 31.januar(2025) til 28.februar(2025),
                forventetStatus = HttpStatusCode.BadRequest,
            ).also { (_, _, response) ->
                response harKode "ugyldig_periode_for_opphør"
                response.shouldContain("Perioden som opphøres må slutte i en gjeldende innvilgelsesperiode")
            }

            oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = 1.februar(2025) til 1.mars(2025),
                forventetStatus = HttpStatusCode.BadRequest,
            ).also { (_, _, response) ->
                response harKode "ugyldig_periode_for_opphør"
                response.shouldContain("Perioden som opphøres må starte i en gjeldende innvilgelsesperiode")
            }
        }
    }
}
