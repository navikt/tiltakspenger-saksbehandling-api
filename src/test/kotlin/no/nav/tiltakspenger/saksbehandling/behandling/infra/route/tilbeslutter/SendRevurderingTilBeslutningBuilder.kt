package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import arrow.core.Tuple4
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

interface SendRevurderingTilBeslutningBuilder {

    /** Oppretter ny sak, søknad og behandling. */
    @Suppress("unused")
    suspend fun ApplicationTestBuilder.sendRevurderingStansTilBeslutning(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Tuple4<Sak, Søknad, RammebehandlingId, String> {
        val (sak, søknad, rammevedtakSøknadsbehandling, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(
            tac,
        )
        val sakId = sak.id
        val revurderingId = revurdering.id

        val søknadsbehandling = rammevedtakSøknadsbehandling.rammebehandling as Søknadsbehandling
        taBehandling(tac, sak.id, revurderingId, saksbehandler)

        oppdaterRevurderingStans(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
            begrunnelseVilkårsvurdering = null,
            fritekstTilVedtaksbrev = null,
            valgteHjemler = setOf(HjemmelForStans.Alder),
            stansFraOgMed = søknadsbehandling.vedtaksperiode!!.fraOgMed,
            harValgtStansFraFørsteDagSomGirRett = false,
        )

        return Tuple4(
            sak,
            søknad,
            revurderingId,
            sendRevurderingTilBeslutningForBehandlingId(
                tac,
                sakId,
                revurderingId,
                saksbehandler,
            ),
        )
    }

    /** Oppretter ny sak, søknad og behandling. */
    suspend fun ApplicationTestBuilder.sendRevurderingInnvilgelseTilBeslutning(
        tac: TestApplicationContext,
        søknadsbehandlingInnvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        revurderingInnvilgelsesperioder: Innvilgelsesperioder = søknadsbehandlingInnvilgelsesperioder,
        oppdatertTiltaksdeltakelse: Tiltaksdeltakelse? = tiltaksdeltakelse(revurderingInnvilgelsesperioder.totalPeriode),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Tuple4<Sak, Søknad, Rammevedtak, String> {
        val (sak, søknad, rammevedtakSøknadsbehandling, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse(
            tac,
            søknadsbehandlingInnvilgelsesperioder = søknadsbehandlingInnvilgelsesperioder,
            oppdatertTiltaksdeltakelse = oppdatertTiltaksdeltakelse,
        )

        oppdaterRevurderingInnvilgelse(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
            begrunnelseVilkårsvurdering = null,
            fritekstTilVedtaksbrev = null,
            innvilgelsesperioder = revurderingInnvilgelsesperioder,
            barnetillegg = Barnetillegg.utenBarnetillegg(revurderingInnvilgelsesperioder.perioder),
        )

        return Tuple4(
            sak,
            søknad,
            rammevedtakSøknadsbehandling,
            sendRevurderingTilBeslutningForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
                saksbehandler,
            ),
        )
    }

    /** Forventer at det allerede finnes en behandling med status `UNDER_BEHANDLING` */
    suspend fun ApplicationTestBuilder.sendRevurderingTilBeslutningForBehandlingId(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): String {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/behandling/$behandlingId/sendtilbeslutning",
            jwt = jwt,
            forventet = forventet,
        ).apply {
            val bodyAsText = this.body
            return bodyAsText
        }
    }
}
