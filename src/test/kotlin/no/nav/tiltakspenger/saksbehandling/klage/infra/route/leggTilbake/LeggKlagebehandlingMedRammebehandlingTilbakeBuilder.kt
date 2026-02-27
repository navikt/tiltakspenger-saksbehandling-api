package no.nav.tiltakspenger.saksbehandling.klage.infra.route.leggTilbake

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
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.leggKlagebehandlingTilbake
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.leggTilbake.leggTilbakeKlagebehandlingRoute]
 */
interface LeggKlagebehandlingMedRammebehandlingTilbakeBuilder {

    /**
     * Kun for saksbehandler, beslutter må via rammebehandlingen.
     * Krever at begge behandlingene er UNDER_BEHANDLING.
     * Skal sette saksbehandler til null for begge behandlinger og sette status til KLAR_TIL_BEHANDLING.
     *
     * 1. Oppretter ny sak, søknad og iverksetter søknadsbehandling.
     * 2. Starter klagebehandling med godkjente formkrav
     * 3. Vurderer klagebehandling til omgjøring
     * 4. Oppretter rammebehandling for klagebehandlingen
     * 5. Legger klagebehandlingen tilbake
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgLeggKlagebehandlingMedRammebehandlingTilbake(
        tac: TestApplicationContext,
        saksbehandlerSøknadsbehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSøknadsbehandling"),
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        journalpostId: JournalpostId = JournalpostId("12345"),
        erKlagerPartISaken: Boolean = true,
        klagesDetPåKonkreteElementerIVedtaket: Boolean = true,
        erKlagefristenOverholdt: Boolean = true,
        erUnntakForKlagefrist: KlagefristUnntakSvarord? = null,
        erKlagenSignert: Boolean = true,
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, Rammebehandling, KlagebehandlingDTOJson>? {
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
        val (oppdatertSak, oppdatertKlagebehandling, json) = leggKlagebehandlingTilbake(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = rammebehandlingMedKlagebehandling.klagebehandling!!.id,
            saksbehandler = saksbehandlerKlagebehandling,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        return Triple(oppdatertSak, oppdatertSak.hentRammebehandling(oppdatertKlagebehandling.rammebehandlingId!!)!!, json)
    }
}
