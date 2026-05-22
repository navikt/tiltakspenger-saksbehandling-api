package no.nav.tiltakspenger.saksbehandling.klage.infra.route.gjenoppta

import io.kotest.assertions.json.CompareJsonOptions
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldekortbehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.gjenopptaKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.gjenopptaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSettKlagebehandlingMedMeldekortbehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Klagebehandling gjenoppta route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.gjenoppta.gjenopptaKlagebehandlingRoute]
 * Meldekortbehandling gjenoppta route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.gjenopptaMeldekortbehandlingRoute]
 */
interface GjenopptaKlagebehandlingMedMeldekortbehandlingBuilder {
    /** 1. Oppretter ny sak, søknad og iverksetter søknadsbehandling.
     *  2. Starter klagebehandling med godkjente formkrav
     *  3. Oppdaterer klagebehandlingen til medhold/omgjøring; oppretter meldekortbehandling og knytter den til klagebehandlingen.
     *  4. Setter klagebehandlingen på vent (setter også meldekortbehandlingen på vent)
     *  5. Gjenopptar klagebehandlingen via klage-endepunktet (gjenopptar også meldekortbehandlingen)
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgGjenopptaKlagebehandlingMedMeldekortbehandling(
        tac: TestApplicationContext,
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, MeldekortUnderBehandling, KlagebehandlingDTOJson>? {
        val (sak, meldekortbehandling, _) = this.iverksettSøknadsbehandlingOgSettKlagebehandlingMedMeldekortbehandlingPåVent(
            tac = tac,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
        ) ?: return null
        val klagebehandling = requireNotNull(meldekortbehandling.klagebehandling)
        val (oppdatertSak, _, json) = gjenopptaKlagebehandling(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerKlagebehandling,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        val oppdatertMeldekortbehandling = oppdatertSak.hentMeldekortbehandling(meldekortbehandling.id) as MeldekortUnderBehandling
        return Triple(oppdatertSak, oppdatertMeldekortbehandling, json)
    }

    /** 1. Oppretter ny sak, søknad og iverksetter søknadsbehandling.
     *  2. Starter klagebehandling med godkjente formkrav
     *  3. Oppdaterer klagebehandlingen til medhold/omgjøring; oppretter meldekortbehandling og knytter den til klagebehandlingen.
     *  4. Setter klagebehandlingen på vent (setter også meldekortbehandlingen på vent)
     *  5. Gjenopptar via meldekortbehandling-endepunktet (gjenopptar også klagebehandlingen)
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgGjenopptaMeldekortbehandlingForKlage(
        tac: TestApplicationContext,
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, MeldekortUnderBehandling, MeldekortbehandlingDTOJson>? {
        val (sak, meldekortbehandling, _) = this.iverksettSøknadsbehandlingOgSettKlagebehandlingMedMeldekortbehandlingPåVent(
            tac = tac,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
        ) ?: return null
        val (oppdatertSak, oppdatertMeldekortbehandling, json) = gjenopptaMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortbehandling.id,
            saksbehandlerEllerBeslutter = saksbehandlerKlagebehandling,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        return Triple(oppdatertSak, oppdatertMeldekortbehandling as MeldekortUnderBehandling, json)
    }
}
