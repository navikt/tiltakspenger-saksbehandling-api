package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.taOgOverta

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.RammebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.json.JSONObject

/**
 * Gjelder for både søknadsbehandling og revurdering.
 */
interface OvertaRammebehandlingBuilder {

    /**
     * Forventer at det allerede finnes en behandling.
     * Denne fungerer både for saksbehandler og beslutter.
     */
    suspend fun ApplicationTestBuilder.overtaBehanding(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: RammebehandlingId,
        overtarFra: String,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Triple<Sak, Rammebehandling, RammebehandlingDTOJson> {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.PATCH,
            "/sak/$sakId/behandling/$behandlingId/overta",
            jwt = jwt,
            forventet = ForventetRespons(status = 200),
            body = """{"overtarFra":"$overtarFra"}""",
        ).apply {
            val bodyAsText = this.body

            val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            val behandling = tac.behandlingContext.rammebehandlingRepo.hent(behandlingId)
            val behandlingJson = JSONObject(bodyAsText)

            return Triple(sak, behandling, behandlingJson)
        }
    }
}
