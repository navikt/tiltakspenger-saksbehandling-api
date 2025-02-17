package no.nav.tiltakspenger.vedtak.routes.behandling.start

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
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.vedtak.routes.RouteBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import org.json.JSONObject

interface StartBehandlingBuilder {

    /** Oppretter ny sak, søknad og behandling. */
    suspend fun ApplicationTestBuilder.startBehandling(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Triple<Sak, Søknad, BehandlingId> {
        val (sak, søknad) = opprettSakOgSøknad(tac)
        return Triple(sak, søknad, startBehandlingForSøknadId(tac, sak.id, søknad.id))
    }

    /** Forventer at det allerede finnes en sak og søknad. */
    suspend fun ApplicationTestBuilder.startBehandlingForSøknadId(
        tac: TestApplicationContext,
        sakId: SakId,
        søknadId: SøknadId,
    ): BehandlingId {
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/soknad/$søknadId/startbehandling")
            },
            jwt = tac.jwtGenerator.createJwtForSaksbehandler(),
        ).apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe HttpStatusCode.OK
            }
            return BehandlingId.fromString(JSONObject(bodyAsText).getString("id"))
        }
    }
}
