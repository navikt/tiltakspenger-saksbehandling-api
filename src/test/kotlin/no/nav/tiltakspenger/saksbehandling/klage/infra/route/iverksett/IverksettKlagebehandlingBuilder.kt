package no.nav.tiltakspenger.saksbehandling.klage.infra.route.iverksett

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
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagevedtakForKlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingTilAvvisningBrevtekst
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.iverksett.iverksettAvvistKlagebehandlingRoute]
 */
interface IverksettKlagebehandlingBuilder {
    /** 1. Oppretter ny sak
     *  2. Starter klagebehandling til avvisning
     *  3. Oppdaterer brevtekst
     *  4. Iverksetter
     */
    suspend fun ApplicationTestBuilder.opprettSakOgIverksettKlagebehandling(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, Klagevedtak, KlagebehandlingDTOJson>? {
        val (sak, klagebehandling, _) = this.opprettSakOgOppdaterKlagebehandlingTilAvvisningBrevtekst(
            tac = tac,
            saksbehandler = saksbehandler,
            fnr = fnr,
        ) ?: return null
        return iverksettKlagebehandlingForSakId(
            tac = tac,
            sakId = sak.id,
            klagebehandlingId = klagebehandling.id,
            saksbehandler = saksbehandler,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        )
    }

    /**
     * Forventer at det allerede finnes en sak.
     * Emulerer journalføring av vedtaksbrev.
     */
    suspend fun ApplicationTestBuilder.iverksettKlagebehandlingForSakId(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Triple<Sak, Klagevedtak, KlagebehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Patch,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/klage/$klagebehandlingId/iverksett")
            },
            jwt = jwt,
        ).apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                if (forventetStatus != null) status shouldBe forventetStatus
            }

            if (forventetJsonBody != null) {
                bodyAsText.shouldEqualJson(forventetJsonBody)
            }
            if (status != HttpStatusCode.OK) return null
            // Emulerer journalføring og distribuering av vedtaksbrev
            tac.klagebehandlingContext.journalførKlagevedtakService.journalførAvvisningbrev()
            tac.klagebehandlingContext.distribuerKlagevedtaksbrevService.distribuerAvvisningsbrev()
            val jsonObject: KlagebehandlingDTOJson = objectMapper.readTree(bodyAsText)
            val klagebehandlingId = KlagebehandlingId.fromString(jsonObject.get("id").asString())
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!

            return Triple(
                oppdatertSak,
                oppdatertSak.hentKlagevedtakForKlagebehandlingId(klagebehandlingId),
                jsonObject,
            )
        }
    }
}
