package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdaterForBeslutter

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SkalJournalføreNotat
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdaterRammebehandlingForBeslutterRoute]
 */
interface OppdaterRammebehandlingForBeslutterBuilder {

    /** Forventer at det allerede finnes en behandling med status `UNDER_BESLUTNING`. */
    suspend fun ApplicationTestBuilder.oppdaterRammebehandlingForBeslutter(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: RammebehandlingId,
        skalJournalføreNotat: SkalJournalføreNotat,
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): String? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = beslutter)
        tac.leggTilBruker(jwt, beslutter)
        val response = defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/behandling/$behandlingId/beslutter/oppdater",
            jwt = jwt,
            forventet = forventet,
            body = """{"skalJournalføreNotat": {"vilkårsvurdering": ${skalJournalføreNotat.vilkårsvurdering}, "barnetillegg": ${skalJournalføreNotat.barnetillegg}}}""",
        )
        if (response.statusCode != 200) return null
        return response.body
    }
}
