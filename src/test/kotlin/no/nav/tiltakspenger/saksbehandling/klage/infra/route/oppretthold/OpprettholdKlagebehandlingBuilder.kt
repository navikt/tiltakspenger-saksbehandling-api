package no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppretthold

import io.kotest.assertions.json.CompareJsonOptions
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
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
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
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
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
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
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
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
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
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
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
        utførJobber: Boolean = true,
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequestWithAssertions(
            HttpMethod.Patch,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/klage/$klagebehandlingId/oppretthold")
            },
            jwt = jwt,
            forventet = forventetStatus?.let { ForventetRespons(status = it) },
        ).apply {
            val bodyAsText = this.bodyAsText()

            if (forventetJsonBody != null) {
                bodyAsText.shouldEqualJson(forventetJsonBody)
            }
            if (status != HttpStatusCode.OK) return null
            if (utførJobber) {
                // Emulerer journalføring og distribuering av innstillingbrev + oversendelse til klageinstansen.
                tac.klagebehandlingContext.journalførKlagebrevJobb.journalførInnstillingsbrev()
                tac.klagebehandlingContext.distribuerKlagebrevJobb.distribuerInnstillingsbrev()
                tac.klagebehandlingContext.oversendKlageTilKlageinstansJobb.oversendKlagerTilKlageinstans()
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
