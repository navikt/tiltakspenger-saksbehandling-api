package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.avbryt

import arrow.core.Tuple4
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.SakDTOJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.behandling.infra.route.rammebehandlingRoutes]
 */
interface AvbrytRammebehandlingBuilder {
    /**
     * 1. Oppretter ny sak og søknad
     * 2. Starter søknadsbehandling under behandling
     * 3. Avbryter
     */
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingOgAvbryt(
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
        return avbrytRammebehandling(
            tac = tac,
            saksnummer = sak.saksnummer,
            rammebehandlingId = søknadsbehandling.id,
            saksbehandler = saksbehandler,
            forventet = forventet,
            sakId = sak.id,
        )
    }

    /**
     * Forventer at det allerede finnes en sak og en åpen rammebehandling under behanding.
     */
    suspend fun ApplicationTestBuilder.avbrytRammebehandling(
        tac: TestApplicationContext,
        saksnummer: Saksnummer,
        sakId: SakId,
        rammebehandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        begrunnelse: String = "begrunnelse for avbryt søknad og/eller rammebehandling",
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple4<Sak, Søknad, Rammebehandling?, SakDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$saksnummer/avbryt-aktiv-behandling",
            jwt = jwt,
            forventet = forventet,
            body =
            """
                {
                "behandlingId": "$rammebehandlingId",
                "begrunnelse": "$begrunnelse"
                }
            """.trimIndent(),
        ).apply {
            val bodyAsText = this.body

            if (statusCode != 200) return null
            val sakJson: SakDTOJson = objectMapper.readTree(bodyAsText)

            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Tuple4(
                oppdatertSak,
                oppdatertSak.søknader.single(),
                rammebehandlingId.let { oppdatertSak.hentRammebehandling(it)!! },
                sakJson,
            )
        }
    }
}
