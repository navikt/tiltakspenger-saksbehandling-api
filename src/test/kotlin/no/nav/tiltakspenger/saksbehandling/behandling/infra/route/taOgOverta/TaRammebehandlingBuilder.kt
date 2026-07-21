package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.taOgOverta

import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.RammebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.json.JSONObject

interface TaRammebehandlingBuilder {

    /**
     * Forventer at det allerede finnes en behandling.
     * Denne fungerer både for saksbehandler og beslutter.
     * Returnerer null dersom responsen ikke er 200 OK.
     */
    suspend fun ApplicationTestBuilder.taBehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetBody: String? = null,
    ): Triple<Sak, AttesterbarBehandling, RammebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        val response = defaultRequestWithAssertions(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/behandling/$behandlingId/ta")
            },
            jwt = jwt,
            forventet = forventetStatus?.let { status ->
                ForventetRespons(status = status, body = forventetBody?.let { ForventetBody.Json(it) })
            },
        )
        if (response.status != HttpStatusCode.OK) return null

        val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
        val behandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)

        return Triple(sak, behandling, JSONObject(response.bodyAsText()))
    }
}
