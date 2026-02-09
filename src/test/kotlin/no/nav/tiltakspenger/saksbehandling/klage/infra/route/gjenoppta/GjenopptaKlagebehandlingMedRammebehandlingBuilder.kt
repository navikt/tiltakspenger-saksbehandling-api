package no.nav.tiltakspenger.saksbehandling.klage.infra.route.gjenoppta

import io.kotest.assertions.json.CompareJsonOptions
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import no.nav.tiltakspenger.saksbehandling.klage.domene.åpneRammebehandlingerMedKlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.gjenopptaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Klagebehandling route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.gjenoppta.gjenopptaKlagebehandlingRoute]
 * Rammebehandling route: [no.nav.tiltakspenger.saksbehandling.behandling.infra.route.gjenopptaRammebehandling]
 */
interface GjenopptaKlagebehandlingMedRammebehandlingBuilder {
    /** 1. Oppretter ny sak, søknad og iverksetter søknadsbehandling.
     *  2. Starter klagebehandling med godkjente formkrav
     *  3. Oppdaterer klagebehandlingen til medhold/omgjøring; oppretter rammebehandling og knytter den til klagebehandlingen.
     *  4. Setter klagebehandlingen på vent
     *  5. Gjenopptar klagebehandlingen
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgGjenopptaKlagebehandlingMedRammebehandling(
        tac: TestApplicationContext,
        saksbehandlerSøknadsbehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSøknadsbehandling"),
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        journalpostId: JournalpostId = JournalpostId("12345"),
        erKlagefristenOverholdt: Boolean = true,
        erUnntakForKlagefrist: KlagefristUnntakSvarord? = null,
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, Rammebehandling, KlagebehandlingDTOJson>? {
        val (sak, rammebehandlingMedSøknadsbehandling, _) = this.iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent(
            tac = tac,
            saksbehandlerSøknadsbehandling = saksbehandlerSøknadsbehandling,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
            journalpostId = journalpostId,
            erKlagefristenOverholdt = erKlagefristenOverholdt,
            erUnntakForKlagefrist = erUnntakForKlagefrist,
        ) ?: return null
        val klagebehandling = rammebehandlingMedSøknadsbehandling.klagebehandling!!
        val (oppdatertSak, oppdatertKlagebehandling, sakJson) = gjenopptaKlagebehandling(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerKlagebehandling,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        val oppdatertRammebehandling = oppdatertSak.åpneRammebehandlingerMedKlagebehandlingId(oppdatertKlagebehandling.id).first()
        val klagebehandlingJson = sakJson.get("klageBehandlinger").first()
        return Triple(oppdatertSak, oppdatertRammebehandling, klagebehandlingJson)
    }
}
