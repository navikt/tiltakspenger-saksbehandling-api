package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother

interface MigrerTilOppdragsversjon3RouteBuilder {

    /**
     * Migrerer saken fra oppdragsversjon 2 til 3.
     * Returnerer null når [forventet] er en feilrespons, jf. route-byggermønsteret i AGENTS.md.
     */
    suspend fun ApplicationTestBuilder.migrerTilOppdragsversjon3(
        tac: TestApplicationContext,
        sakId: SakId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons = ForventetRespons(status = 200),
        opphevSperrenStraks: Boolean = false,
    ): String? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)

        val respons = defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/migrer-til-oppdragsversjon-3${if (opphevSperrenStraks) "?opphevSperrenStraks=true" else ""}",
            jwt = jwt,
            forventet = forventet,
        )

        return if (forventet.status == 200) respons.body else null
    }
}
