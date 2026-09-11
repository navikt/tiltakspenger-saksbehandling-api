package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.taOgOverta

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import tools.jackson.databind.JsonNode
import kotlin.to

interface TaRammebehandlingBuilder {

    /**
     * Forventer at det allerede finnes en eller flere behandlinger.
     * Denne fungerer både for saksbehandler og beslutter.
     * Returnerer null dersom responsen ikke er 200 OK.
     */
    suspend fun ApplicationTestBuilder.taRammebehandlinger(
        tac: TestApplicationContext,
        behandlinger: List<Pair<SakId, RammebehandlingId>>,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        returnerSaker: Boolean? = null,
        forventet: ForventetRespons = ForventetRespons(status = 200, contentType = "application/json; charset=UTF-8"),
    ): Pair<List<Pair<Sak, Rammebehandling>>, JsonNode?>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(token = jwt, bruker = saksbehandler)

        val behandlingerJson = behandlinger.joinToString(", ") { (sakId, behandlingId) ->
            """{ "behandlingId": "$behandlingId", "sakId": "$sakId" }"""
        }

        val returnerSakerJson = returnerSaker?.let { """, "returnerSaker": $it""" } ?: ""

        val response = defaultRequestWithAssertions(
            method = HttpMethod.POST,
            uri = "/behandlinger/ta",
            jwt = jwt,
            forventet = forventet,
            body = """
                { "behandlinger": [ $behandlingerJson ]$returnerSakerJson }
            """.trimIndent(),
        )

        if (response.statusCode != 200) return null

        val rammebehandlingerRespons = behandlinger.map { (sakId, behandlingId) ->
            val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            val behandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
            (sak to behandling)
        }

        return Pair(rammebehandlingerRespons, objectMapper.readTree(response.body))
    }
}
