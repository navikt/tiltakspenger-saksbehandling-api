package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import io.kotest.assertions.json.shouldEqualJson
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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.RammebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.json.JSONObject

/**
 * Gjelder for både søknadsbehandling og revurdering.
 */
interface IverksettRammebehandlingBuilder {

    /** Forventer at det allerede finnes en behandling med status `UNDER_BESLUTNING` */
    suspend fun ApplicationTestBuilder.iverksettForBehandlingId(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, Rammebehandling, RammebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = beslutter,
        )
        tac.texasClient.leggTilBruker(jwt, beslutter)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/behandling/$behandlingId/iverksett")
            },
            jwt = jwt,
        ).apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe forventetStatus
            }
            if (forventetJsonBody != null) {
                bodyAsText.shouldEqualJson(forventetJsonBody)
            }
            if (status != HttpStatusCode.OK) return null

            val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            val behandling = sak.rammebehandlinger.hentBehandling(behandlingId)!!
            return Triple(sak, behandling, JSONObject(bodyAsText))
        }
    }
}
