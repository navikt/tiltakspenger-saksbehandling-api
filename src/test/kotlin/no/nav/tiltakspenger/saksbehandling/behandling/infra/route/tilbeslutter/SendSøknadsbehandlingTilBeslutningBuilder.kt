package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import arrow.core.Tuple4
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedAvslag
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse

interface SendSøknadsbehandlingTilBeslutningBuilder {

    /** Oppretter ny sak (hvis sakId er null), søknad og behandling. */
    suspend fun ApplicationTestBuilder.sendSøknadsbehandlingTilBeslutning(
        tac: TestApplicationContext,
        sakId: SakId? = null,
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        resultat: SøknadsbehandlingsresultatType = SøknadsbehandlingsresultatType.INNVILGELSE,
        innvilgelsesperioder: Innvilgelsesperioder = innvilgelsesperioder(),
        tiltaksdeltakelse: Tiltaksdeltakelse = tiltaksdeltakelse(innvilgelsesperioder.totalPeriode),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperioder.perioder),
    ): Tuple4<Sak, Søknad, BehandlingId, String> {
        val (sak, søknad, behandling) = when (resultat) {
            SøknadsbehandlingsresultatType.INNVILGELSE -> opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
                tac = tac,
                sakId = sakId,
                fnr = fnr,
                saksbehandler = saksbehandler,
                innvilgelsesperioder = innvilgelsesperioder,
                barnetillegg = barnetillegg,
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            SøknadsbehandlingsresultatType.AVSLAG -> opprettSøknadsbehandlingUnderBehandlingMedAvslag(
                tac,
                fnr,
                saksbehandler,
            )
        }

        val sakId = sak.id
        val behandlingId = behandling.id

        return Tuple4(
            sak,
            søknad,
            behandlingId,
            sendSøknadsbehandlingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sakId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
            ),
        )
    }

    /** Forventer at det allerede finnes en behandling med status `UNDER_BEHANDLING` */
    suspend fun ApplicationTestBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
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
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe HttpStatusCode.OK
            }
            return bodyAsText
        }
    }

    suspend fun ApplicationTestBuilder.sendSøknadsbehandlingTilBeslutningReturnerRespons(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): HttpResponse {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        return defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/behandling/$behandlingId/sendtilbeslutning")
            },
            jwt = jwt,
        )
    }
}
