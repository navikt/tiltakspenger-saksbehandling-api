package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

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
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.sak.Sak

suspend fun ApplicationTestBuilder.oppdaterBehandling(
    tac: TestApplicationContext,
    sakId: SakId,
    behandlingId: BehandlingId,
    oppdaterBehandlingDTO: OppdaterBehandlingDTO,
): Triple<Sak, Behandling, String> {
    defaultRequest(
        HttpMethod.Post,
        url {
            protocol = URLProtocol.HTTPS
            path("/sak/$sakId/behandling/$behandlingId/oppdater")
        },
        jwt = tac.jwtGenerator.createJwtForSaksbehandler(),
    ) {
        setBody(
            serialize(oppdaterBehandlingDTO),
        )
    }.apply {
        val bodyAsText = this.bodyAsText()
        withClue(
            "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
        ) {
            status shouldBe HttpStatusCode.OK
        }
        val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
        val behandling = tac.behandlingContext.behandlingRepo.hent(behandlingId)
        return Triple(sak, behandling, bodyAsText)
    }
}
