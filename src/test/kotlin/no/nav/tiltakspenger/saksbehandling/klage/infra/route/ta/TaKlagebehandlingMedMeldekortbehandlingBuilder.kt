package no.nav.tiltakspenger.saksbehandling.klage.infra.route.ta

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldekortbehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.infra.route.SakDTOJson
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.leggTilbake.LeggKlagebehandlingMedMeldekortbehandlingTilbakeBuilder
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taMeldekortbehanding
import no.nav.tiltakspenger.saksbehandling.sak.Sak

interface TaKlagebehandlingMedMeldekortbehandlingBuilder : LeggKlagebehandlingMedMeldekortbehandlingTilbakeBuilder {
    suspend fun ApplicationTestBuilder.tattMeldekortbehandlingMedKlageFraKlageRoute(
        tac: TestApplicationContext,
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        saksbehandlerSomTarKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomTarKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, MeldekortUnderBehandling, SakDTOJson>? {
        val (sak, meldekortbehandlingMedKlagebehandling, _) = lagtTilbakeMeldekortbehandlingMedKlageFraKlageRoute(
            tac = tac,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
        ) ?: return null
        val klagebehandling = meldekortbehandlingMedKlagebehandling.klagebehandling!!
        val (oppdatertSak, _, json) = taKlagebehandling(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandlerSomTar = saksbehandlerSomTarKlagebehandling,
            forventet = forventet,
        ) ?: return null
        val oppdatertMeldekortbehandling = oppdatertSak.hentMeldekortbehandling(meldekortbehandlingMedKlagebehandling.id) as MeldekortUnderBehandling
        return Triple(oppdatertSak, oppdatertMeldekortbehandling, json)
    }

    suspend fun ApplicationTestBuilder.tattMeldekortbehandlingMedKlageFraMeldekortRoute(
        tac: TestApplicationContext,
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        saksbehandlerSomTar: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomTarMeldekort"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, MeldekortUnderBehandling, MeldekortbehandlingDTOJson>? {
        val (sak, meldekortbehandlingMedKlagebehandling, _) = lagtTilbakeMeldekortbehandlingMedKlageFraKlageRoute(
            tac = tac,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
        ) ?: return null
        val (oppdatertSak, oppdatertMeldekortbehandling, json) = taMeldekortbehanding(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortbehandlingMedKlagebehandling.id,
            saksbehandlerEllerBeslutter = saksbehandlerSomTar,
            forventet = forventet,
        ) ?: return null
        return Triple(oppdatertSak, oppdatertMeldekortbehandling as MeldekortUnderBehandling, json)
    }
}
