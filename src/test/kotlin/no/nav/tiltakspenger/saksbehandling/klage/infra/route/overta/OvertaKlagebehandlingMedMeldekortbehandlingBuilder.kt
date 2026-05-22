package no.nav.tiltakspenger.saksbehandling.klage.infra.route.overta

import io.kotest.assertions.json.CompareJsonOptions
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.SakDTOJson
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.opprettMeldekortbehandling.OpprettMeldekortbehandlingForKlageBuilder
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.overtaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak

interface OvertaKlagebehandlingMedMeldekortbehandlingBuilder : OpprettMeldekortbehandlingForKlageBuilder {
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgOvertaKlagebehandlingMedMeldekortbehandling(
        tac: TestApplicationContext,
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        overtarFra: Saksbehandler = saksbehandlerKlagebehandling,
        saksbehandlerSomOvertarKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomOvertarKlagebehandling"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, MeldekortUnderBehandling, SakDTOJson>? {
        val (sak, klagebehandling, meldekortbehandling) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandlingForKlage(
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
}
