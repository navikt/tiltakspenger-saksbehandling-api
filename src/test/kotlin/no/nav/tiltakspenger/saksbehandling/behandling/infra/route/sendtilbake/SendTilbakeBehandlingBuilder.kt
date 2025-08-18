package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.sendtilbake

import arrow.core.Tuple4
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettAutomatiskBehandlingKlarTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehanding
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad

/**
 * Gjelder for både søknadsbehandling og revurdering.
 */
interface SendTilbakeBehandlingBuilder {

    /**
     * Oppretter ny sak, søknad og behandling.
     * Merk at denne tar behandlingen, før den sender tilbake til saksbehandler.
     * */
    suspend fun ApplicationTestBuilder.sendTilbake(
        tac: TestApplicationContext,
        begrunnelse: String = "send_tilbake_begrunnelse",
        beslutter: Saksbehandler = ObjectMother.beslutter(),
    ): Tuple4<Sak, Søknad, BehandlingId, String> {
        val (sak, søknad, behandlingId) = sendSøknadsbehandlingTilBeslutning(tac)
        taBehanding(tac, sak.id, behandlingId, beslutter)
        return Tuple4(sak, søknad, behandlingId, sendTilbakeForBehandlingId(tac, behandlingId, begrunnelse, beslutter))
    }

    suspend fun ApplicationTestBuilder.sendTilbakeAutomatiskSaksbehandletBehandling(
        tac: TestApplicationContext,
        begrunnelse: String = "send_tilbake_begrunnelse",
        beslutter: Saksbehandler = ObjectMother.beslutter(),
    ): Tuple4<Sak, Søknad, BehandlingId, String> {
        val (sak, søknad, behandling) = opprettAutomatiskBehandlingKlarTilBeslutning(tac)
        taBehanding(tac, sak.id, behandling.id, beslutter)
        return Tuple4(sak, søknad, behandling.id, sendTilbakeForBehandlingId(tac, behandling.id, begrunnelse, beslutter))
    }

    /** Forventer at det allerede finnes en behandling med status `UNDER_BESLUTNING` */
    suspend fun ApplicationTestBuilder.sendTilbakeForBehandlingId(
        tac: TestApplicationContext,
        behandlingId: BehandlingId,
        begrunnelse: String = "send_tilbake_begrunnelse",
        beslutter: Saksbehandler = ObjectMother.beslutter(),
    ): String {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = beslutter,
        )
        tac.texasClient.leggTilBruker(jwt, beslutter)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/behandling/sendtilbake/$behandlingId")
            },
            jwt = jwt,
        ) {
            setBody("""{"begrunnelse": "$begrunnelse"}""")
        }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe HttpStatusCode.OK
            }
            return bodyAsText
        }
    }
}
