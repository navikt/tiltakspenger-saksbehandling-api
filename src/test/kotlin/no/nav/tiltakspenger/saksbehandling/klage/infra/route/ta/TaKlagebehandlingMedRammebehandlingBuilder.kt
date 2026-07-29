package no.nav.tiltakspenger.saksbehandling.klage.infra.route.ta

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.SakDTOJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgLeggKlagebehandlingMedRammebehandlingTilbake
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.ta.taKlagebehandlingRoute]
 */
interface TaKlagebehandlingMedRammebehandlingBuilder {
    /** 1. Oppretter ny sak, søknad og iverksetter søknadsbehandling.
     *  2. Starter klagebehandling med godkjente formkrav
     *  3. Legger klagebehandlingen tilbake
     *  4. Tar klagebehandlingen
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgTaKlagebehandlingMedRammebehandling(
        tac: TestApplicationContext,
        saksbehandlerSøknadsbehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSøknadsbehandling"),
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        saksbehandlerSomTarKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomTarKlagebehandling"),
        journalpostId: JournalpostId = JournalpostId("12345"),
        erKlagerPartISaken: Boolean = true,
        klagesDetPåKonkreteElementerIVedtaket: Boolean = true,
        erKlagefristenOverholdt: Boolean = true,
        erUnntakForKlagefrist: KlagefristUnntakSvarord? = null,
        erKlagenSignert: Boolean = true,
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Rammebehandling, SakDTOJson>? {
        val (sak, rammebehandlingMedKlagebehandling, _) = this.iverksettSøknadsbehandlingOgLeggKlagebehandlingMedRammebehandlingTilbake(
            tac = tac,
            saksbehandlerSøknadsbehandling = saksbehandlerSøknadsbehandling,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
            journalpostId = journalpostId,
        ) ?: return null
        val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
        val (oppdatertSak, _, json) = taKlagebehandling(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandlerSomTar = saksbehandlerSomTarKlagebehandling,
            forventet = forventet,
        ) ?: return null
        val oppdatertRammebehandlingMedKlagebehandling =
            oppdatertSak.hentRammebehandling(rammebehandlingMedKlagebehandling.id)!!
        return Triple(oppdatertSak, oppdatertRammebehandlingMedKlagebehandling, json)
    }
}
