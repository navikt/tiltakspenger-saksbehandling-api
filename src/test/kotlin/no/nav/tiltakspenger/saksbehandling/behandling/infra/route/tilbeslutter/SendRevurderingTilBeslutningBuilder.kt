package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import arrow.core.Tuple4
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStansEllerOpphør
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
    ): Tuple4<Sak, Søknad, BehandlingId, String> {
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
            valgteHjemler = setOf(HjemmelForStansEllerOpphør.Alder),
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
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    ): String {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/behandling/$behandlingId/sendtilbeslutning")
            },
            jwt = jwt,
        ).apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                """
                    Response details:
                    Status: ${this.status}
                    Content-Type: ${this.contentType()}
                    Body: $bodyAsText
                """.trimMargin(),
            ) {
                status shouldBe forventetStatus
            }
            return bodyAsText
        }
    }
}
