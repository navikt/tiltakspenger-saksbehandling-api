package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.fritekst

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
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import org.json.JSONObject

/**
 * Gjelder for både søknadsbehandling og revurdering.
 */
interface OppdaterFritekstBuilder {

    /** Oppretter ny sak, søknad og behandling. */
    suspend fun ApplicationTestBuilder.oppdaterFritekst(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        fritekstTilVedtaksbrev: String = "some_tekst",
    ): Tuple4<Sak, Søknad, Behandling, String> {
        val (sak, søknad, behandling) = startSøknadsbehandling(tac)
        val sakId = sak.id
        val (oppdatertSak, oppdatertBehandling, responseJson) = oppdaterFritekstForBehandlingId(
            tac,
            sakId,
            behandling.id,
            saksbehandler,
            fritekstTilVedtaksbrev,
        )
        return Tuple4(oppdatertSak, søknad, oppdatertBehandling, responseJson)
    }

    /** Forventer at det allerede finnes en sak, søknad og behandling. */
    suspend fun ApplicationTestBuilder.oppdaterFritekstForBehandlingId(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        fritekstTilVedtaksbrev: String = "some_tekst",
    ): Triple<Sak, Behandling, String> {
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
            val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            val behandling = tac.behandlingContext.behandlingRepo.hent(behandlingId)
            return Triple(sak, behandling, bodyAsText)
        }
    }
}
