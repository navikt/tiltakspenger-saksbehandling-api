package no.nav.tiltakspenger.saksbehandling.klage.infra.route.avbryt

import io.kotest.assertions.json.CompareJsonOptions
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.opprettMeldekortbehandling.OpprettMeldekortbehandlingForKlageBuilder
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingAvbrutt
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak

interface AvbrytKlagebehandlingMedMeldekortbehandlingBuilder : OpprettMeldekortbehandlingForKlageBuilder {
    /**
     * 1. Iverksetter søknadsbehandling
     * 2. Oppretter klagebehandling vurdert til OMGJØR
     * 3. Oppretter meldekortbehandling knyttet til klagebehandlingen
     * 4. Avbryter meldekortbehandlingen
     * 5. Avbryter klagebehandlingen
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgAvbrytKlagebehandlingMedMeldekortbehandling(
        tac: TestApplicationContext,
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        begrunnelseAvbrytMeldekort: String = "begrunnelse for avbryt meldekortbehandling",
        begrunnelseAvbrytKlage: String = "begrunnelse for avbryt klagebehandling",
        forventetStatusAvbrytKlage: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBodyAvbrytKlage: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, MeldekortbehandlingAvbrutt, Klagebehandling>? {
        val (sak, klagebehandling, meldekortbehandling) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandlingForKlage(
            tac = tac,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
        )

        val (sakEtterAvbrytMeldekort, avbruttMeldekortbehandling, _) = avbrytMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            meldekortId = meldekortbehandling.id,
            begrunnelse = begrunnelseAvbrytMeldekort,
            saksbehandler = saksbehandlerKlagebehandling,
        ) ?: return null

        val (oppdatertSak, avbruttKlagebehandling, _) = avbrytKlagebehandling(
            tac = tac,
            sakId = sakEtterAvbrytMeldekort.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerKlagebehandling,
            begrunnelse = begrunnelseAvbrytKlage,
            forventetStatus = forventetStatusAvbrytKlage,
            forventetJsonBody = forventetJsonBodyAvbrytKlage,
        ) ?: return null

        val oppdatertMeldekortbehandling =
            oppdatertSak.hentMeldekortbehandling(meldekortbehandling.id) as MeldekortbehandlingAvbrutt

        return Triple(oppdatertSak, oppdatertMeldekortbehandling, avbruttKlagebehandling)
    }
}
