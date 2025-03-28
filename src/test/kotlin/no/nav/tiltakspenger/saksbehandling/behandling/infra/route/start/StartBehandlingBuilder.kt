package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start

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
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.april
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.json.JSONObject

interface StartBehandlingBuilder {

    /** Oppretter ny sak, søknad og behandling. */
    suspend fun ApplicationTestBuilder.startBehandling(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        virkingsperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Tuple4<Sak, Søknad, Behandling, String> {
        val (sak, søknad) = opprettSakOgSøknad(tac, fnr, deltakelsesperiode = virkingsperiode)
        val (behandling, response) = startBehandlingForSøknadId(tac, sak.id, søknad.id)
        return Tuple4(sak, søknad, behandling, response)
    }

    /** Forventer at det allerede finnes en sak og søknad. */
    suspend fun ApplicationTestBuilder.startBehandlingForSøknadId(
        tac: TestApplicationContext,
        sakId: SakId,
        søknadId: SøknadId,
    ): Pair<Behandling, String> {
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
            val behandlingId = BehandlingId.fromString(JSONObject(bodyAsText).getString("id"))
            return tac.behandlingContext.behandlingRepo.hent(behandlingId) to bodyAsText
        }
    }
}
