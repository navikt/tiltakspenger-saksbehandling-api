package no.nav.tiltakspenger.saksbehandling.klage.infra.route.overta

import io.kotest.assertions.json.CompareJsonOptions
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.SakDTOJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
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
        erKlagerPartISaken: Boolean = true,
        klagesDetPåKonkreteElementerIVedtaket: Boolean = true,
        erKlagefristenOverholdt: Boolean = true,
        erUnntakForKlagefrist: KlagefristUnntakSvarord? = null,
        erKlagenSignert: Boolean = true,
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, Rammebehandling, SakDTOJson>? {
        val (sak, rammebehandlingMedKlagebehandling, _) = this.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
            tac = tac,
            saksbehandlerSøknadsbehandling = saksbehandlerSøknadsbehandling,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
            journalpostId = journalpostId,
            erKlagerPartISaken = erKlagerPartISaken,
            klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
            erKlagefristenOverholdt = erKlagefristenOverholdt,
            erUnntakForKlagefrist = erUnntakForKlagefrist,
            erKlagenSignert = erKlagenSignert,
        ) ?: return null
        val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
        tac.clock.spol1timeFrem()
        val (oppdatertSak, _, json) = overtaKlagebehandling(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerSomOvertaKlagebehandling,
            overtarFra = overtarFra.navIdent,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        val oppdatertRammebehandlingMedKlagebehandling =
            oppdatertSak.hentRammebehandling(rammebehandlingMedKlagebehandling.id)!!
        return Triple(oppdatertSak, oppdatertRammebehandlingMedKlagebehandling, json)
    }
}
