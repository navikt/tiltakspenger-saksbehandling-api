package no.nav.tiltakspenger.saksbehandling.klage.infra.route.leggTilbake

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldekortbehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.leggKlagebehandlingTilbake
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.leggTilbakeMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.sak.Sak

interface LeggKlagebehandlingMedMeldekortbehandlingTilbakeBuilder {
    suspend fun ApplicationTestBuilder.lagtTilbakeMeldekortbehandlingMedKlageFraKlageRoute(
        tac: TestApplicationContext,
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, MeldekortUnderBehandling, KlagebehandlingDTOJson>? {
        val (sak, klagebehandling, meldekortbehandling) = opprettMeldekortbehandlingForKlage(
            tac = tac,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
        )
        val (oppdatertSak, _, json) = leggKlagebehandlingTilbake(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerKlagebehandling,
            forventet = forventet,
        ) ?: return null
        val oppdatertMeldekortbehandling = oppdatertSak.hentMeldekortbehandling(meldekortbehandling.id) as MeldekortUnderBehandling
        return Triple(oppdatertSak, oppdatertMeldekortbehandling, json)
    }

    suspend fun ApplicationTestBuilder.lagtTilbakeMeldekortbehandlingMedKlageFraMeldekortRoute(
        tac: TestApplicationContext,
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, MeldekortUnderBehandling, MeldekortbehandlingDTOJson>? {
        val (sak, _, meldekortbehandling) = opprettMeldekortbehandlingForKlage(
            tac = tac,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
        )
        val (oppdatertSak, oppdatertMeldekortbehandling, json) = leggTilbakeMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortbehandling.id,
            saksbehandlerEllerBeslutter = saksbehandlerKlagebehandling,
            forventet = forventet,
        ) ?: return null

        return Triple(oppdatertSak, oppdatertMeldekortbehandling as MeldekortUnderBehandling, json)
    }
}
