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
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.sendFørstegangsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.taBehanding
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad

interface SendTilbakeBuilder {

    /**
     * Oppretter ny sak, søknad og behandling.
     * Merk at denne tar behandlingen, før den sender tilbake til saksbehandler.
     * */
    suspend fun ApplicationTestBuilder.sendTilbake(
        tac: TestApplicationContext,
        begrunnelse: String = "send_tilbake_begrunnelse",
        beslutter: Saksbehandler = ObjectMother.beslutter(),
    ): Tuple4<Sak, Søknad, BehandlingId, String> {
        val (sak, søknad, behandlingId) = sendFørstegangsbehandlingTilBeslutning(tac)
        taBehanding(tac, behandlingId, beslutter)
        return Tuple4(sak, søknad, behandlingId, sendTilbakeForBehandlingId(tac, behandlingId, begrunnelse, beslutter))
    }

    /** Forventer at det allerede finnes en behandling med status `UNDER_BESLUTNING` */
    suspend fun ApplicationTestBuilder.sendTilbakeForBehandlingId(
        tac: TestApplicationContext,
        behandlingId: BehandlingId,
        begrunnelse: String = "send_tilbake_begrunnelse",
        beslutter: Saksbehandler = ObjectMother.beslutter(),
    ): String {
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/behandling/sendtilbake/$behandlingId")
            },
            jwt = tac.jwtGenerator.createJwtForSaksbehandler(
                saksbehandler = beslutter,
            ),
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
