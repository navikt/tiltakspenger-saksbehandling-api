package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat.Stans
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import org.junit.jupiter.api.Test

class OppdaterRevurderingStansRouteTest {

    @Test
    fun `kan oppdatere revurdering stans`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(
                tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 30.april(2025)),
            )

            oppdaterRevurderingStans(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                fritekstTilVedtaksbrev = "ny brevtekst",
                begrunnelseVilkårsvurdering = "ny begrunnelse",
                stansFraOgMed = 9.april(2025),
                harValgtStansFraFørsteDagSomGirRett = false,
            )

            val oppdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id)

            oppdatertBehandling.resultat.shouldBeInstanceOf<Stans>()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
            oppdatertBehandling.vedtaksperiode!!.fraOgMed shouldBe 9.april(2025)
            (oppdatertBehandling.resultat as Stans).valgtHjemmel shouldBe listOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak)
        }
    }

    @Test
    fun `kan oppdatere revurdering stans over utbetalte perioder`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(
                tac,
                innvilgelsesperioder = innvilgelsesperioder(1.april(2025) til 30.april(2025)),
            )

            oppdaterRevurderingStans(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                fritekstTilVedtaksbrev = "ny brevtekst",
                begrunnelseVilkårsvurdering = "ny begrunnelse",
                valgteHjemler = setOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
                stansFraOgMed = 9.april(2025),
                harValgtStansFraFørsteDagSomGirRett = false,
            )

            val oppdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id)

            oppdatertBehandling.resultat.shouldBeInstanceOf<Stans>()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
            oppdatertBehandling.vedtaksperiode!!.fraOgMed shouldBe 9.april(2025)
            (oppdatertBehandling.resultat as Stans).valgtHjemmel shouldBe listOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak)
        }
    }

    @Test
    fun `oppdater revurdering stans feiler hvis stansFraOgMed er før innvilgelsesperioden`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)

            val stansFraOgMed = sak.førsteDagSomGirRett!!.minusDays(2)

            oppdaterRevurderingStans(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                begrunnelseVilkårsvurdering = null,
                fritekstTilVedtaksbrev = null,
                valgteHjemler = setOf(ValgtHjemmelForStans.Alder),
                stansFraOgMed = stansFraOgMed,
                harValgtStansFraFørsteDagSomGirRett = false,
                forventetStatus = HttpStatusCode.InternalServerError,
            )
        }
    }

    @Test
    fun `oppdater revurdering stans feiler hvis stansFraOgMed er etter innvilgelsesperioden`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)
            val stansFraOgMed = sak.sisteDagSomGirRett!!.plusDays(2)

            oppdaterRevurderingStans(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                begrunnelseVilkårsvurdering = null,
                fritekstTilVedtaksbrev = null,
                valgteHjemler = setOf(ValgtHjemmelForStans.Alder),
                stansFraOgMed = stansFraOgMed,
                harValgtStansFraFørsteDagSomGirRett = false,
                forventetStatus = HttpStatusCode.InternalServerError,
            )
        }
    }

    @Test
    fun `revurdering stans over hull i innvilgelsesperiodene`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(
                tac,
                innvilgelsesperioder = innvilgelsesperioder(
                    1.januar(2026) til 10.januar(2026),
                    15.januar(2026) til 30.januar(2026),
                ),
            )

            val (_, oppdatertStans) = oppdaterRevurderingStans(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                begrunnelseVilkårsvurdering = null,
                fritekstTilVedtaksbrev = null,
                valgteHjemler = setOf(ValgtHjemmelForStans.Alder),
                stansFraOgMed = 9.januar(2026),
                harValgtStansFraFørsteDagSomGirRett = false,
            )

            oppdatertStans.vedtaksperiode shouldBe (9.januar(2026) til 30.januar(2026))
        }
    }
}
