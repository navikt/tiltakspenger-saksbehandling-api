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
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.SakDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingManuell
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgBeslutterTarBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.overtaMeldekortbehandlingRoute]
 * Dto: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortbehandlingDTO]
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
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortUnderBehandling, SakDTOJson>? {
        val (_, søknad, rammevedtakSøknadsbehandling, opprettetMeldekortbehandling) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(
            tac = tac,
            saksbehandler = overtarFraSaksbehandler,
        ) ?: return null

        val (oppdatertSak, oppdatertMeldekortbehandling, json) = overtaMeldekortbehandling(
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
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortbehandlingManuell, SakDTOJson>? {
        val (_, søknad, rammevedtakSøknadsbehandling, opprettetMeldekortbehandling) = iverksettSøknadsbehandlingOgBeslutterTarBehandling(
            tac = tac,
            beslutter = overtarFraBeslutter,
        ) ?: return null

        val (oppdatertSak, oppdatertMeldekortbehandling, json) = overtaMeldekortbehandling(
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
            oppdatertMeldekortbehandling as MeldekortbehandlingManuell,
            json,
        )
    }

    /**
     * Forventer at det det finnes en sak med en meldeperiode som gir rett til tiltakspenger
     * @return Dersom statusen er UNDER_BEHANDLING returneres [MeldekortUnderBehandling], ellers hvis statusen er UNDER_BESLUTNING returneres [MeldekortbehandlingManuell]
     */
    suspend fun ApplicationTestBuilder.overtaMeldekortbehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        overtarFraSaksbehandlerEllerBeslutter: Saksbehandler = ObjectMother.saksbehandlerOgBeslutter("overtarFraSaksbehandlerEllerBeslutter"),
        saksbehandlerEllerBeslutterSomOvertar: Saksbehandler = ObjectMother.saksbehandlerOgBeslutter("saksbehandlerEllerBeslutterSomOvertar"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, Meldekortbehandling, SakDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandlerEllerBeslutterSomOvertar)
        tac.leggTilBruker(jwt, saksbehandlerEllerBeslutterSomOvertar)
        defaultRequestWithAssertions(
            HttpMethod.Patch,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/meldekort/$meldekortId/overta")
            },
            jwt = jwt,
            forventet = forventetStatus?.let { status ->
                ForventetRespons(
                    status = status,
                    body = forventetJsonBody?.let { ForventetBody.Json(it) },
                    contentType = ContentType.parse("application/json; charset=UTF-8"),
                )
            },
        ) {
            this.setBody("""{"overtarFra":"${overtarFraSaksbehandlerEllerBeslutter.navIdent}"}""")
        }.apply {
            val bodyAsText = this.bodyAsText()
            if (status != HttpStatusCode.OK) return null
            val jsonObject: SakDTOJson = objectMapper.readTree(bodyAsText)
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Triple(
                oppdatertSak,
                oppdatertSak.hentMeldekortbehandling(meldekortId)!!,
                jsonObject,
            )
        }
    }
}
