package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother

interface ToggleKanSendeHelgForMeldekortSakRouteBuilder {

    /**
     * Slår brukerens mulighet til å melde helg av eller på.
     * Returnerer null når [forventet] er en feilrespons, jf. route-byggermønsteret i AGENTS.md.
     */
    suspend fun ApplicationTestBuilder.toggleKanSendeHelgForMeldekort(
        tac: TestApplicationContext,
        sakId: SakId,
        kanSendeHelg: Boolean,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventet: ForventetRespons = ForventetRespons(status = 200),
    ): String? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)

        val respons = defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/toggle-helg-meldekort",
            jwt = jwt,
            forventet = forventet,
            body = """{"kanSendeHelg": $kanSendeHelg}""",
        )

        return if (forventet.status == 200) respons.body else null
    }
}
