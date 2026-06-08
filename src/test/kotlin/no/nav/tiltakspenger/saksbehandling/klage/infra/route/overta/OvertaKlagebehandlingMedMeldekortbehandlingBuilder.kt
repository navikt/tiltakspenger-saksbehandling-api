package no.nav.tiltakspenger.saksbehandling.klage.infra.route.overta

import io.kotest.assertions.json.CompareJsonOptions
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldekortbehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.infra.route.SakDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.overtaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.overtaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak

interface OvertaKlagebehandlingMedMeldekortbehandlingBuilder {
    suspend fun ApplicationTestBuilder.overtattMeldekortbehandlingMedKlageFraKlageRoute(
        tac: TestApplicationContext,
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        overtarFra: Saksbehandler = saksbehandlerKlagebehandling,
        saksbehandlerSomOvertarKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomOvertarKlagebehandling"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, MeldekortUnderBehandling, SakDTOJson>? {
        val (sak, klagebehandling, meldekortbehandling) = opprettMeldekortbehandlingForKlage(
            tac = tac,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
        )
        tac.clock.spol1timeFrem()
        val (oppdatertSak, _, json) = overtaKlagebehandling(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerSomOvertarKlagebehandling,
            overtarFra = overtarFra.navIdent,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        val oppdatertMeldekortbehandling = oppdatertSak.hentMeldekortbehandling(meldekortbehandling.id) as MeldekortUnderBehandling
        return Triple(oppdatertSak, oppdatertMeldekortbehandling, json)
    }

    suspend fun ApplicationTestBuilder.overtattMeldekortbehandlingMedKlageFraMeldekortRoute(
        tac: TestApplicationContext,
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        saksbehandlerSomOvertarKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomOvertarKlagebehandling"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, MeldekortUnderBehandling, MeldekortbehandlingDTOJson>? {
        val (sak, _, meldekortbehandling) = opprettMeldekortbehandlingForKlage(
            tac = tac,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
        )
        tac.clock.spol1timeFrem()
        val (oppdatertSak, oppdatertMeldekortbehandling, json) = overtaMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortbehandling.id,
            overtarFraSaksbehandlerEllerBeslutter = saksbehandlerKlagebehandling,
            saksbehandlerEllerBeslutterSomOvertar = saksbehandlerSomOvertarKlagebehandling,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        return Triple(oppdatertSak, oppdatertMeldekortbehandling as MeldekortUnderBehandling, json)
    }
}
