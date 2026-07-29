package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.gjenoppta

import arrow.core.Tuple4
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.SakDTOJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settRammebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.behandling.infra.route.gjenopptaRammebehandling]
 */
interface GjenopptaRammebehandlingBuilder {
    /**
     * 1. Oppretter ny sak og søknad
     * 2. Starter søknadsbehandling under behandling
     * 3. Setter rammebehandling på vent
     * 4. Gjenopptar rammebehandlingen
     */
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingOgGjenoppta(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple4<Sak, Søknad, Rammebehandling?, SakDTOJson>? {
        val (sak, _, søknadsbehandling) = this.opprettSøknadsbehandlingUnderBehandling(
            tac = tac,
            saksbehandler = saksbehandler,
            fnr = fnr,
        )
        // Først setter vi behandlingen på vent
        settRammebehandlingPåVent(
            tac = tac,
            sakId = sak.id,
            rammebehandlingId = søknadsbehandling.id,
            saksbehandler = saksbehandler,
        )

        return gjenopptaRammebehandling(
            tac = tac,
            sakId = sak.id,
            rammebehandlingId = søknadsbehandling.id,
            saksbehandler = saksbehandler,
            forventet = forventet,
        )
    }

    /**
     * Forventer at det allerede finnes en sak og en åpen rammebehandling som er satt på vent.
     */
    suspend fun ApplicationTestBuilder.gjenopptaRammebehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        rammebehandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple4<Sak, Søknad, Rammebehandling, SakDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/behandling/$rammebehandlingId/gjenoppta",
            jwt = jwt,
            forventet = forventet,
        ).apply {
            val bodyAsText = this.body

            if (statusCode != 200) return null
            val sakJson: SakDTOJson = objectMapper.readTree(bodyAsText)

            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Tuple4(
                oppdatertSak,
                oppdatertSak.søknader.last(),
                oppdatertSak.hentRammebehandling(rammebehandlingId)!!,
                sakJson,
            )
        }
    }
}
