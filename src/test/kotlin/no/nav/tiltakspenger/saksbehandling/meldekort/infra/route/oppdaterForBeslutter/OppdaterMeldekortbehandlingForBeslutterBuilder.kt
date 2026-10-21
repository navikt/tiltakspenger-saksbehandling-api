package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.oppdaterForBeslutter

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.oppdaterMeldekortbehandlingForBeslutterRoute]
 */
interface OppdaterMeldekortbehandlingForBeslutterBuilder {

    /** Forventer at det allerede finnes en meldekortbehandling med status `UNDER_BESLUTNING`. */
    suspend fun ApplicationTestBuilder.oppdaterMeldekortbehandlingForBeslutter(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        skalJournalføreNotat: Boolean,
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): String? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = beslutter)
        tac.leggTilBruker(jwt, beslutter)
        val response = defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/meldekort/$meldekortId/beslutter/oppdater",
            jwt = jwt,
            forventet = forventet,
            body = """{"skalJournalføreNotat": $skalJournalføreNotat}""",
        )
        if (response.statusCode != 200) return null
        return response.body
    }
}
