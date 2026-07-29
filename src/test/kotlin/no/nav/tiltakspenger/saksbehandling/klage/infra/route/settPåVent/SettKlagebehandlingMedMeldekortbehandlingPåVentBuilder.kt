package no.nav.tiltakspenger.saksbehandling.klage.infra.route.settPåVent

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.SakDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settMeldekortbehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taMeldekortbehanding
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.LocalDate

interface SettKlagebehandlingMedMeldekortbehandlingPåVentBuilder {

    suspend fun ApplicationTestBuilder.meldekortbehandlingMedKlageSattPåVentFraKlageRoute(
        tac: TestApplicationContext,
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, MeldekortUnderBehandling, SakDTOJson>? {
        val (sak, klagebehandling, meldekortbehandling) = opprettMeldekortbehandlingForKlage(
            tac = tac,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
        )
        val (oppdatertSak, _, json) = settKlagebehandlingPåVent(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerKlagebehandling,
            forventet = forventet,
        ) ?: return null
        val oppdatertMeldekortbehandling =
            oppdatertSak.hentMeldekortbehandling(meldekortbehandling.id) as MeldekortUnderBehandling
        return Triple(oppdatertSak, oppdatertMeldekortbehandling, json)
    }

    suspend fun ApplicationTestBuilder.meldekortbehandlingMedKlagebehandlingSattPåVentFraMeldekortRoute(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        begrunnelse: String = "begrunnelse for å sette klage på vent",
        frist: LocalDate? = 14.januar(2025),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, MeldekortUnderBehandling, SakDTOJson>? {
        val (sak, _, meldekortbehandling) = opprettMeldekortbehandlingForKlage(
            tac = tac,
            saksbehandlerKlagebehandling = saksbehandler,
        )
        val (oppdatertSak, oppdatertMeldekortbehandling, json) = settMeldekortbehandlingPåVent(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortbehandling.id,
            saksbehandlerEllerBeslutter = saksbehandler,
            begrunnelse = begrunnelse,
            frist = frist,
            forventet = forventet,
        ) ?: return null
        return Triple(oppdatertSak, oppdatertMeldekortbehandling as MeldekortUnderBehandling, json)
    }

    suspend fun ApplicationTestBuilder.meldekortbehandlingUnderBeslutningMedKlagebehandlingSattPåVentFraMeldekortRoute(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
        begrunnelse: String = "begrunnelse for å sette meldekort på vent",
        frist: LocalDate? = 14.januar(2025),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): Triple<Sak, MeldekortbehandlingManuell, SakDTOJson>? {
        val (sak, _, meldekortbehandling) = opprettMeldekortbehandlingForKlage(
            tac = tac,
            saksbehandlerKlagebehandling = saksbehandler,
        )

        oppdaterMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortbehandling.id,
            saksbehandler = saksbehandler,
        ) ?: return null

        val (_, meldekortTilBeslutning, _) = sendMeldekortbehandlingTilBeslutning(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortbehandling.id,
            saksbehandler = saksbehandler,
        ) ?: return null

        taMeldekortbehanding(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortTilBeslutning.id,
            saksbehandlerEllerBeslutter = beslutter,
        ) ?: return null

        val (oppdatertSak, oppdatertMeldekortbehandling, json) = settMeldekortbehandlingPåVent(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortbehandling.id,
            saksbehandlerEllerBeslutter = beslutter,
            begrunnelse = begrunnelse,
            frist = frist,
            forventet = forventet,
        ) ?: return null
        return Triple(oppdatertSak, oppdatertMeldekortbehandling as MeldekortbehandlingManuell, json)
    }
}
