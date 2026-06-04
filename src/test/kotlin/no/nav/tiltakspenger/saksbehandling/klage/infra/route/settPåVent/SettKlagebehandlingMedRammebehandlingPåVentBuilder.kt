package no.nav.tiltakspenger.saksbehandling.klage.infra.route.settPåVent

import io.kotest.assertions.json.CompareJsonOptions
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.infra.route.SakDTOJson
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettetSøknadsbehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settRammebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.LocalDate

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
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, Rammebehandling, KlagebehandlingDTOJson>? {
        val (sak, rammebehandlingMedSøknadsbehandling, _) = this.opprettetSøknadsbehandlingForKlage(
            tac = tac,
            saksbehandlerSøknadsbehandling = saksbehandlerSøknadsbehandling,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
            journalpostId = journalpostId,
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

    /**
     * Setter rammebehandling (med tilknyttet klagebehandling) på vent via rammebehandling-ruten.
     * Rammebehandlingen er i status UNDER_BESLUTNING.
     * Setter også klagebehandlingen på vent (via beslutteren).
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgSettRammebehandlingMedKlagebehandlingPåVentFraUnderBeslutning(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
        begrunnelse: String = "begrunnelse for å sette rammebehandling på vent",
        frist: LocalDate? = 14.januar(2025),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
    ): Triple<Sak, Rammebehandling, SakDTOJson>? {
        val (sak, rammebehandling, _) = opprettetSøknadsbehandlingForKlage(
            tac = tac,
            saksbehandlerKlagebehandling = saksbehandler,
        ) ?: return null

        oppdaterSøknadsbehandlingInnvilgelse(
            tac = tac,
            sakId = sak.id,
            behandlingId = rammebehandling.id,
            saksbehandler = saksbehandler,
        )

        sendSøknadsbehandlingTilBeslutningForBehandlingId(
            tac = tac,
            sakId = sak.id,
            behandlingId = rammebehandling.id,
            saksbehandler = saksbehandler,
        )

        taBehandling(
            tac = tac,
            sakId = sak.id,
            behandlingId = rammebehandling.id,
            saksbehandler = beslutter,
        )

        val (oppdatertSak, _, oppdatertRammebehandling, json) = settRammebehandlingPåVent(
            tac = tac,
            sakId = sak.id,
            rammebehandlingId = rammebehandling.id,
            saksbehandler = beslutter,
            begrunnelse = begrunnelse,
            frist = frist,
            forventetStatus = forventetStatus,
        ) ?: return null

        return Triple(oppdatertSak, oppdatertRammebehandling, json)
    }
}
