package no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppretthold

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettMeldekortvedtakOgOppdaterKlagebehandlingTilOpprettholdelseBrevtekst
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingTilOpprettholdelseBrevtekst
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.iverksett.iverksettAvvistKlagebehandlingRoute]
 */
interface OpprettholdKlagebehandlingBuilder {
    /** 1. Iverksetter en søknadsbehandling og meldekortbehandling.
     *  2. Starter klagebehandling med vedtakDetKlagesPå = meldekortvedtak
     *  3. Vurderer til opprettholdelse
     *  4. Oppdaterer brevtekst
     *  5. Opprettholder (emulerer journalføring, distribuering av vedtaksbrev og oversendelse til klageinstansen)
     */
    suspend fun ApplicationTestBuilder.iverksettMeldekortvedtakOgOpprettholdKlagebehandling(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandlerMeldekortbehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerMeldekortbehandling"),
        saksbehandlerKlagebehandling: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        utførJobber: Boolean = true,
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, _, klagebehandling, _) = this.iverksettMeldekortvedtakOgOppdaterKlagebehandlingTilOpprettholdelseBrevtekst(
            tac = tac,
            saksbehandlerMeldekortbehandling = saksbehandlerMeldekortbehandling,
            saksbehandlerKlagebehandling = saksbehandlerKlagebehandling,
            fnr = fnr,
        ) ?: return null
        return opprettholdKlagebehandlingForSakId(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandlerKlagebehandling,
            forventet = forventet,
            utførJobber = utførJobber,
        )
    }

    /** 1. Oppretter ny sak
     *  2. Starter klagebehandling til opprettholdelse
     *  3. Oppdaterer brevtekst
     *  4. Opprettholder (emulerer journalføring, distribuering av vedtaksbrev og oversendelse til klageinstansen)
     */
    suspend fun ApplicationTestBuilder.opprettSakOgOpprettholdKlagebehandling(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        utførJobber: Boolean = true,
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, _, klagebehandling, _) = this.opprettSakOgOppdaterKlagebehandlingTilOpprettholdelseBrevtekst(
            tac = tac,
            saksbehandler = saksbehandler,
            fnr = fnr,
        ) ?: return null
        return opprettholdKlagebehandlingForSakId(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandler,
            forventet = forventet,
            utførJobber = utførJobber,
        )
    }

    /**
     * Forventer at det allerede finnes en sak.
     * Emulerer journalføring og distribuering av innstillingsbrev + oversendelse til klageinstansen.
     */
    suspend fun ApplicationTestBuilder.opprettholdKlagebehandlingForSakId(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        utførJobber: Boolean = true,
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.PATCH,
            "/sak/$sakId/klage/$klagebehandlingId/oppretthold",
            jwt = jwt,
            forventet = forventet,
        ).apply {
            val bodyAsText = this.body

            if (statusCode != 200) return null
            if (utførJobber) {
                // Emulerer journalføring og distribuering av innstillingbrev + oversendelse til klageinstansen, kun for denne klagebehandlingen/saken.
                tac.klagebehandlingContext.journalførKlagebrevJobb.journalførInnstillingsbrev(klagebehandlingId)
                tac.klagebehandlingContext.distribuerKlagebrevJobb.distribuerInnstillingsbrev(klagebehandlingId)
                tac.klagebehandlingContext.oversendKlageTilKlageinstansJobb.oversendKlagerTilKlageinstansForSak(sakId)
            }
            val jsonObject: KlagebehandlingDTOJson = objectMapper.readTree(bodyAsText)
            val klagebehandlingId = KlagebehandlingId.fromString(jsonObject.get("id").asString())
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!

            return Triple(
                oppdatertSak,
                oppdatertSak.hentKlagebehandling(klagebehandlingId),
                jsonObject,
            )
        }
    }
}
