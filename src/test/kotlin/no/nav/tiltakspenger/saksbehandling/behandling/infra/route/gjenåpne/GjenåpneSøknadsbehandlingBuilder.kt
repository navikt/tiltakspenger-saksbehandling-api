package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.gjenåpne

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.json.JSONObject

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.behandling.infra.route.gjenåpneSøknadsbehandlingRoute]
 */
interface GjenåpneSøknadsbehandlingBuilder {
    /**
     * Forventer at det allerede finnes en sak med en avbrutt søknadsbehandling.
     * @return saken slik den ser ut etter gjenåpningen, og den nye søknadsbehandlingen.
     * Null dersom kallet ikke gikk gjennom.
     */
    suspend fun ApplicationTestBuilder.gjenåpneSøknadsbehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        avbruttBehandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        begrunnelse: String? = "søknaden ble avbrutt ved en feil",
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Pair<Sak, Søknadsbehandling>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        val response = defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/behandling/$avbruttBehandlingId/gjenapne",
            jwt = jwt,
            forventet = forventet,
            body = if (begrunnelse == null) """{}""" else """{"begrunnelse":"$begrunnelse"}""",
        )
        if (response.statusCode != 200) return null

        val nyBehandlingId = RammebehandlingId.fromString(JSONObject(response.body).getString("id"))
        val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
        return oppdatertSak to (oppdatertSak.hentRammebehandling(nyBehandlingId) as Søknadsbehandling)
    }
}
