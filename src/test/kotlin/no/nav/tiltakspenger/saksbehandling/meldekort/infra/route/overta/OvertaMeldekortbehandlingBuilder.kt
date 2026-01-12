package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.overta

import arrow.core.Tuple5
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldekortBehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgBeslutterTarBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.json.JSONObject

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.overtaMeldekortBehandlingRoute]
 * Dto: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortBehandlingDTO]
 */
interface OvertaMeldekortbehandlingBuilder {

    /**
     * 1. Iverksetter en søknadsbehandling
     * 2. Oppretter en meldekortbehandling for saksbehandler 1
     * 3. Saksbehandler 2 overtar meldekortbehandlingen
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgOverta(
        tac: TestApplicationContext,
        overtarFraSaksbehandler: Saksbehandler = ObjectMother.saksbehandler("overtarFraSaksbehandler"),
        saksbehandlerSomOvertar: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerSomOvertar"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortUnderBehandling, MeldekortBehandlingDTOJson>? {
        val (_, søknad, rammevedtakSøknadsbehandling, opprettetMeldekortbehandling) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(
            tac = tac,
            saksbehandler = overtarFraSaksbehandler,
        ) ?: return null

        val (oppdatertSak, oppdatertMeldekortbehandling, json) = overtaMeldekortBehandling(
            tac = tac,
            sakId = opprettetMeldekortbehandling.sakId,
            meldekortId = opprettetMeldekortbehandling.id,
            overtarFraSaksbehandlerEllerBeslutter = overtarFraSaksbehandler,
            saksbehandlerEllerBeslutterSomOvertar = saksbehandlerSomOvertar,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        return Tuple5(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            oppdatertMeldekortbehandling as MeldekortUnderBehandling,
            json,
        )
    }

    /**
     * 1. Iverksetter en søknadsbehandling
     * 2. Oppretter en meldekortbehandling
     * 3. Oppdaterer meldekortbehandlingen
     * 4. Sender meldekortbehandlingen til beslutning
     * 5. Beslutter 1 tar meldekortbehandlingen
     * 6. Beslutter 2 overtar meldekortbehandlingen
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutterOgOverta(
        tac: TestApplicationContext,
        overtarFraBeslutter: Saksbehandler = ObjectMother.beslutter("overtarFraBeslutter"),
        beslutterSomOvertar: Saksbehandler = ObjectMother.beslutter("beslutterSomOvertar"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortBehandletManuelt, MeldekortBehandlingDTOJson>? {
        val (_, søknad, rammevedtakSøknadsbehandling, opprettetMeldekortbehandling) = iverksettSøknadsbehandlingOgBeslutterTarBehandling(
            tac = tac,
            beslutter = overtarFraBeslutter,
        ) ?: return null

        val (oppdatertSak, oppdatertMeldekortbehandling, json) = overtaMeldekortBehandling(
            tac = tac,
            sakId = opprettetMeldekortbehandling.sakId,
            meldekortId = opprettetMeldekortbehandling.id,
            overtarFraSaksbehandlerEllerBeslutter = overtarFraBeslutter,
            saksbehandlerEllerBeslutterSomOvertar = beslutterSomOvertar,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        return Tuple5(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            oppdatertMeldekortbehandling as MeldekortBehandletManuelt,
            json,
        )
    }

    /**
     * Forventer at det det finnes en sak med en meldeperiode som gir rett til tiltakspenger
     * @return Dersom statusen er UNDER_BEHANDLING returneres [MeldekortUnderBehandling], ellers hvis statusen er UNDER_BESLUTNING returneres [no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt]
     */
    suspend fun ApplicationTestBuilder.overtaMeldekortBehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        overtarFraSaksbehandlerEllerBeslutter: Saksbehandler = ObjectMother.saksbehandlerOgBeslutter("overtarFraSaksbehandlerEllerBeslutter"),
        saksbehandlerEllerBeslutterSomOvertar: Saksbehandler = ObjectMother.saksbehandlerOgBeslutter("saksbehandlerEllerBeslutterSomOvertar"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, MeldekortBehandling, MeldekortBehandlingDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandlerEllerBeslutterSomOvertar)
        tac.leggTilBruker(jwt, saksbehandlerEllerBeslutterSomOvertar)
        defaultRequest(
            HttpMethod.Patch,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/meldekort/$meldekortId/overta")
            },
            jwt = jwt,
        ) {
            this.setBody("""{"overtarFra":"${overtarFraSaksbehandlerEllerBeslutter.navIdent}"}""")
        }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                if (forventetStatus != null) status shouldBe forventetStatus
                if (forventetJsonBody != null) bodyAsText.shouldEqualJson(forventetJsonBody)
            }
            if (status != HttpStatusCode.OK) return null
            val jsonObject: MeldekortBehandlingDTOJson = JSONObject(bodyAsText)
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Triple(
                oppdatertSak,
                oppdatertSak.hentMeldekortBehandling(meldekortId)!!,
                jsonObject,
            )
        }
    }
}
