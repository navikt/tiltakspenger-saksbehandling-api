package no.nav.tiltakspenger.saksbehandling.klage.infra.route.ta

import arrow.core.Tuple5
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.SakDTOJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgLeggKlagebehandlingTilbake
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.ta.taKlagebehandlingRoute]
 */
interface TaKlagebehandlingBuilder {
    /** 1. Oppretter ny sak, søknad og iverksetter søknadsbehandling.
     *  2. Starter klagebehandling med godkjente formkrav
     *  3. Legger klagebehandlingen tilbake
     *  4. Tar klagebehandlingen
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgTaKlagebehandling(
        tac: TestApplicationContext,
        saksbehandlerSøknadsbehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSøknadsbehandling"),
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        journalpostId: JournalpostId = JournalpostId("12345"),
        erKlagerPartISaken: Boolean = true,
        klagesDetPåKonkreteElementerIVedtaket: Boolean = true,
        erKlagefristenOverholdt: Boolean = true,
        erUnntakForKlagefrist: KlagefristUnntakSvarord? = null,
        erKlagenSignert: Boolean = true,
        saksbehandlerSomTar: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomTar"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Tuple5<Sak, Søknad, Rammevedtak, Klagebehandling, SakDTOJson>? {
        val (sak, søknad, rammevedtakSøknadsbehandling, klagebehandling, _) = this.iverksettSøknadsbehandlingOgLeggKlagebehandlingTilbake(
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
        val (oppdatertSak, oppdatertKlagebehandling, json) = taKlagebehandling(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandlerSomTar = saksbehandlerSomTar,
            forventet = forventet,
        ) ?: return null
        return Tuple5(oppdatertSak, søknad, rammevedtakSøknadsbehandling, oppdatertKlagebehandling, json)
    }

    /** Forventer at det allerede finnes en sak og åpen klagebehandling. */
    suspend fun ApplicationTestBuilder.taKlagebehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        saksbehandlerSomTar: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, Klagebehandling, SakDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandlerSomTar)
        tac.leggTilBruker(jwt, saksbehandlerSomTar)
        defaultRequestWithAssertions(
            HttpMethod.PATCH,
            "/sak/$sakId/klage/$klagebehandlingId/ta",
            jwt = jwt,
            forventet = forventet,
        ).apply {
            val bodyAsText = this.body
            if (statusCode != 200) return null
            val sakDTOJson: SakDTOJson = objectMapper.readTree(bodyAsText)
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Triple(
                oppdatertSak,
                oppdatertSak.hentKlagebehandling(klagebehandlingId),
                sakDTOJson,
            )
        }
    }
}
