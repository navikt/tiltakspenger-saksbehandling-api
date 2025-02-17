package no.nav.tiltakspenger.vedtak.routes.behandling

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
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import org.json.JSONObject

interface OppdaterFritekstBuilder {
    suspend fun ApplicationTestBuilder.oppdaterFritekst(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        fritekstTilVedtaksbrev: String = "some_tekst",
    ): String {
        defaultRequest(
            HttpMethod.Patch,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/behandling/$behandlingId/fritekst")
            },
            jwt = tac.jwtGenerator.createJwtForSaksbehandler(),
        ) { setBody("""{"fritekst": "$fritekstTilVedtaksbrev"}""") }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe HttpStatusCode.OK
                JSONObject(bodyAsText).getString("fritekstTilVedtaksbrev") shouldBe fritekstTilVedtaksbrev
            }
            return bodyAsText
        }
    }
}
