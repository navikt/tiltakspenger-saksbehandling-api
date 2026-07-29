package no.nav.tiltakspenger.saksbehandling.klage.infra.route.overta

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.SakDTOJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettetSøknadsbehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.overtaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.overta.overtaKlagebehandlingRoute]
 */
interface OvertaKlagebehandlingMedRammebehandlingBuilder {
    /** 1. Oppretter ny sak, søknad og iverksetter søknadsbehandling.
     *  2. Starter klagebehandling med godkjente formkrav
     *  4. Overta klagebehandlingen
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgOvertaKlagebehandlingMedRammebehandling(
        tac: TestApplicationContext,
        saksbehandlerSøknadsbehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSøknadsbehandling"),
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        overtarFra: Saksbehandler = saksbehandlerKlagebehandling,
        saksbehandlerSomOvertaKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomOvertarKlagebehandling"),
        journalpostId: JournalpostId = JournalpostId("12345"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Rammebehandling, SakDTOJson>? {
        val (sak, rammebehandlingMedKlagebehandling, _) = this.opprettetSøknadsbehandlingForKlage(
            tac = tac,
            saksbehandlerSøknadsbehandling = saksbehandlerSøknadsbehandling,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
            journalpostId = journalpostId,
        ) ?: return null
        val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
        tac.clock.spol1timeFrem()
        val (oppdatertSak, _, json) = overtaKlagebehandling(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerSomOvertaKlagebehandling,
            overtarFra = overtarFra.navIdent,
            forventet = forventet,
        ) ?: return null
        val oppdatertRammebehandlingMedKlagebehandling =
            oppdatertSak.hentRammebehandling(rammebehandlingMedKlagebehandling.id)!!
        return Triple(oppdatertSak, oppdatertRammebehandlingMedKlagebehandling, json)
    }
}
