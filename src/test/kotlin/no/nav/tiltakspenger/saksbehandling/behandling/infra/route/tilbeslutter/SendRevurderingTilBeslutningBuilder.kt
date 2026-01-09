package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import arrow.core.Tuple4
import arrow.core.nonEmptyListOf
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
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForStansDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioderDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
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

        val søknadsbehandling = rammevedtakSøknadsbehandling.behandling as Søknadsbehandling
        taBehandling(tac, sak.id, revurderingId, saksbehandler)

        oppdaterBehandling(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
            oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                begrunnelseVilkårsvurdering = null,
                fritekstTilVedtaksbrev = null,
                valgteHjemler = nonEmptyListOf(ValgtHjemmelForStansDTO.Alder),
                stansFraOgMed = søknadsbehandling.vedtaksperiode!!.fraOgMed,
                stansTilOgMed = null,
                harValgtStansFraFørsteDagSomGirRett = false,
                harValgtStansTilSisteDagSomGirRett = true,
            ),
        )

        return Tuple4(
            sak,
            søknad,
            revurderingId,
            sendRevurderingStansTilBeslutningForBehandlingId(
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
        søknadsbehandlingInnvilgelsesperiode: Periode = 1.til(10.april(2025)),
        revurderingInnvilgelsesperiode: Periode = søknadsbehandlingInnvilgelsesperiode.plusTilOgMed(14L),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Tuple4<Sak, Søknad, Rammevedtak, String> {
        val (sak, søknad, rammevedtakSøknadsbehandling, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse(
            tac,
            søknadsbehandlingInnvilgelsesperiode = søknadsbehandlingInnvilgelsesperiode,
            revurderingVedtaksperiode = revurderingInnvilgelsesperiode,
        )

        oppdaterBehandling(
            tac = tac,
            sakId = sak.id,
            behandlingId = revurdering.id,
            oppdaterBehandlingDTO = OppdaterRevurderingDTO.Innvilgelse(
                begrunnelseVilkårsvurdering = null,
                fritekstTilVedtaksbrev = null,
                innvilgelsesperioder = revurdering.innvilgelsesperioderDTO(
                    revurderingInnvilgelsesperiode,
                ),
                barnetillegg = Barnetillegg.utenBarnetillegg(revurderingInnvilgelsesperiode).toBarnetilleggDTO(),
            ),
        )

        return Tuple4(
            sak,
            søknad,
            rammevedtakSøknadsbehandling,
            sendRevurderingInnvilgelseTilBeslutningForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
                saksbehandler,
            ),
        )
    }

    /** Forventer at det allerede finnes en behandling med status `UNDER_BEHANDLING` */
    suspend fun ApplicationTestBuilder.sendRevurderingStansTilBeslutningForBehandlingId(
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

    /** Forventer at det allerede finnes en behandling med status `UNDER_BEHANDLING` */
    suspend fun ApplicationTestBuilder.sendRevurderingInnvilgelseTilBeslutningForBehandlingId(
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
