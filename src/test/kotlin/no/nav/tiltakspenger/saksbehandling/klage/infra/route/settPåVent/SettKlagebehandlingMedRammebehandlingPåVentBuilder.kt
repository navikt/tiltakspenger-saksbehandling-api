package no.nav.tiltakspenger.saksbehandling.klage.infra.route.settPåVent

import io.kotest.assertions.json.CompareJsonOptions
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.settPåVent.settKlagebehandlingPåVentRoute]
 */
interface SettKlagebehandlingMedRammebehandlingPåVentBuilder {
    /** 1. Oppretter ny sak, søknad og iverksetter søknadsbehandling.
     *  2. Starter klagebehandling med godkjente formkrav
     *  3. Oppdaterer klagebehandlingen til å ha rammebehandling
     *  4. Setter klagebehandlingen på vent
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent(
        tac: TestApplicationContext,
        saksbehandlerSøknadsbehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSøknadsbehandling"),
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        journalpostId: JournalpostId = JournalpostId("12345"),
        erKlagefristenOverholdt: Boolean = true,
        erUnntakForKlagefrist: KlagefristUnntakSvarord? = null,
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, Rammebehandling, KlagebehandlingDTOJson>? {
        val (sak, rammebehandlingMedSøknadsbehandling, _) = this.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
            tac = tac,
            saksbehandlerSøknadsbehandling = saksbehandlerSøknadsbehandling,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
            journalpostId = journalpostId,
            erKlagefristenOverholdt = erKlagefristenOverholdt,
            erUnntakForKlagefrist = erUnntakForKlagefrist,
        ) ?: return null
        val (oppdatertSak, _, json) = settKlagebehandlingPåVent(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = rammebehandlingMedSøknadsbehandling.klagebehandling!!.id,
            saksbehandler = saksbehandlerKlagebehandling,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        val oppdatertRammebehandlingMedKlagebehandling =
            oppdatertSak.hentRammebehandling(rammebehandlingMedSøknadsbehandling.id)!!
        return Triple(oppdatertSak, oppdatertRammebehandlingMedKlagebehandling, json)
    }
}
