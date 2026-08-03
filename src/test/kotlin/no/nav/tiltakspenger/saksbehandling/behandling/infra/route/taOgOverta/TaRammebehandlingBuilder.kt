package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.taOgOverta

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.SakDTOJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak

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
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, AttesterbarBehandling, SakDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        val response = defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/behandling/$behandlingId/ta",
            jwt = jwt,
            forventet = forventet,
        )
        if (response.statusCode != 200) return null

        val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
        val behandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)

        return Triple(sak, behandling, objectMapper.readTree(response.body))
    }
}
