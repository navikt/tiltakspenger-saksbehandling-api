package no.nav.tiltakspenger.saksbehandling.klage.infra.route.innstillingsbrev

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.journalpost.DokumentInfoId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.ferdigstiltOpprettholdtKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak

interface VisInnstillingsbrevKlagebehandlingBuilder {

    suspend fun ApplicationTestBuilder.opprettSakOgVisinnstillingsbrevForKlagebehandling(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/pdf"),
    ): Triple<Sak, Klagebehandling, ByteArray>? {
        val (sak, klagebehandling) = this.ferdigstiltOpprettholdtKlagebehandling(
            tac = tac,
            saksbehandler = saksbehandler,
            fnr = fnr,
        ) ?: return null

        return visInnstillingsbrevForKlagebehandling(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            dokumentInfoId = (klagebehandling.resultat as Klagebehandlingsresultat.Opprettholdt).dokumentInfoIder.single(),
            saksbehandler = saksbehandler,
            forventet = forventet,
        )
    }

    suspend fun ApplicationTestBuilder.visInnstillingsbrevForKlagebehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        dokumentInfoId: DokumentInfoId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/pdf"),
    ): Triple<Sak, Klagebehandling, ByteArray>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.GET,
            "/sak/$sakId/klage/$klagebehandlingId/innstillingsbrev/$dokumentInfoId",
            jwt = jwt,
            forventet = forventet,
        ).apply {
            val pdf = this.bytes

            if (statusCode != 200) return null
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!

            return Triple(
                oppdatertSak,
                oppdatertSak.hentKlagebehandling(klagebehandlingId),
                pdf,
            )
        }
    }
}
