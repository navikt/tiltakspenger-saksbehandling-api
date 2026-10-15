package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Prioritering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.harKode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperiode
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringOpphør
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringIkkeValgt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringOpphør
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import org.junit.jupiter.api.Test

class OppdaterRevurderingOpphørRouteTest {

    @Test
    fun `skal oppdatere revurdering med opphør av hele vedtaket med brev`() {
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
                skalSendeVedtaksbrev = true,
                prioriteringBegrunnelse = "Opphør haster",
            )

            oppdatertOmgjøring.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringOpphør>()
            oppdatertOmgjøring.resultat!!.vedtaksperiode shouldBe innvilgelsesperiode
            oppdatertOmgjøring.skalSendeVedtaksbrev shouldBe true
            oppdatertOmgjøring.prioritering shouldBe Prioritering.create("Opphør haster")
        }
    }

    @Test
    fun `skal oppdatere revurdering med opphør av hele vedtaket uten brev`() {
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
                skalSendeVedtaksbrev = false,
            )

            oppdatertOmgjøring.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringOpphør>()
            oppdatertOmgjøring.resultat!!.vedtaksperiode shouldBe innvilgelsesperiode
            oppdatertOmgjøring.skalSendeVedtaksbrev shouldBe false
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
                prioriteringBegrunnelse = "Haster",
            )

            oppdatertOmgjøring.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringIkkeValgt>()
            oppdatertOmgjøring.prioritering shouldBe Prioritering.create("Haster")
            oppdatertOmgjøring.resultat!!.vedtaksperiode shouldBe null
            // Ikke valgt beholder vedtakene og periodene fra forrige resultat.
            oppdatertOmgjøring.resultat!!.omgjørRammevedtak.rammevedtakIDer.single() shouldBe søknadVedtak.id
            oppdatertOmgjøring.resultat!!.omgjørRammevedtak.perioder.single() shouldBe opphørsperiode
        }
    }

    @Test
    fun `skal ikke kunne opphøre over en stans`() {
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
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ).also { (_, _, response) ->
                response harKode "kan_ikke_opphøre_over_stans_eller_opphør"
            }

            oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = stansetPeriode,
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ).also { (_, _, response) ->
                response harKode "kan_ikke_opphøre_over_stans_eller_opphør"
            }
        }
    }

    @Test
    fun `skal ikke kunne opphøre et stansvedtak`() {
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
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ).also { (_, _, response) ->
                response harKode "kan_ikke_opphøre_over_stans_eller_opphør"
            }
        }
    }

    @Test
    fun `skal ikke kunne opphøre over et rent opphør`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)

            val (sak, _, søknadvedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            iverksettOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadvedtak.id,
                vedtaksperiode = 1.mars(2025) til 31.mars(2025),
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
                vedtaksperiode = innvilgelsesperiode,
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ).also { (_, _, response) ->
                response harKode "kan_ikke_opphøre_over_stans_eller_opphør"
            }
        }
    }

    @Test
    fun `kan opphøre over et delvis opphør`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)

            val (sak, _, søknadvedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            // Omgjøringen innvilger januar og mars, og opphører dermed februar.
            val (_, delvisOpphør) = iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadvedtak.id,
                vedtaksperiode = innvilgelsesperiode,
                innvilgelsesperioder = innvilgelsesperioder(
                    1.januar(2025) til 31.januar(2025),
                    1.mars(2025) til 31.mars(2025),
                ),
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = delvisOpphør.id,
            )!!

            val (_, oppdatertOmgjøring) = oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = innvilgelsesperiode,
            )

            oppdatertOmgjøring.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringOpphør>()
            oppdatertOmgjøring.resultat!!.vedtaksperiode shouldBe innvilgelsesperiode
            oppdatertOmgjøring.resultat!!.omgjørRammevedtak.rammevedtakIDer shouldBe listOf(delvisOpphør.id)
        }
    }

    @Test
    fun `kan opphøre over flere vedtak`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val revurdertPeriode = 1.februar(2025) til 28.februar(2025)

            val (sak, _, søknadvedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            val (_, revurderingsvedtak) = iverksettRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                innvilgelsesperioder = innvilgelsesperioder(
                    revurdertPeriode,
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(2),
                ),
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadvedtak.id,
            )!!

            val (_, oppdatertOmgjøring) = oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = 15.januar(2025) til 15.mars(2025),
            )

            oppdatertOmgjøring.resultat!!.omgjørRammevedtak shouldBe OmgjørRammevedtak(
                Omgjøringsperiode(
                    rammevedtakId = søknadvedtak.id,
                    periode = 15.januar(2025) til 31.januar(2025),
                    omgjøringsgrad = Omgjøringsgrad.DELVIS,
                ),
                Omgjøringsperiode(
                    rammevedtakId = revurderingsvedtak.id,
                    periode = revurdertPeriode,
                    omgjøringsgrad = Omgjøringsgrad.HELT,
                ),
                Omgjøringsperiode(
                    rammevedtakId = søknadvedtak.id,
                    periode = 1.mars(2025) til 15.mars(2025),
                    omgjøringsgrad = Omgjøringsgrad.DELVIS,
                ),
            )
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
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ).also { (_, _, response) ->
                response harKode "ugyldig_periode_for_opphør"
            }

            oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = 31.januar(2025) til 28.februar(2025),
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ).also { (_, _, response) ->
                response harKode "ugyldig_periode_for_opphør"
                response.shouldContain("Perioden som opphøres må slutte i en gjeldende innvilgelsesperiode")
            }

            oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = 1.februar(2025) til 1.mars(2025),
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ).also { (_, _, response) ->
                response harKode "ugyldig_periode_for_opphør"
                response.shouldContain("Perioden som opphøres må starte i en gjeldende innvilgelsesperiode")
            }
        }
    }
}
