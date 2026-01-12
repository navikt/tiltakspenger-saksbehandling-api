package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.underkjenn

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
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldeperiodeKjedeDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgBeslutterTarBehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.json.JSONObject

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.underkjennMeldekortBehandlingRoute]
 * Dto: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.MeldekortBehandlingDTO]
 */
interface UnderkjennMeldekortbehandlingBuilder {
    /**
     * 1. Iverksetter en søknadsbehandling
     * 2. Oppretter en meldekortbehandling
     * 3. Oppdaterer behandlingen
     * 4. Sender til beslutning
     * 5. Beslutter tar behandlingen.
     * 6. Beslutter underkjenner.
     */
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgUnderkjennMeldekortbehandling(
        tac: TestApplicationContext,
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
        begrunnelse: String = "begrunnelse for underkjennelse",
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortUnderBehandling, MeldeperiodeKjedeDTOJson>? {
        val (sakMedMeldekortbehandlingUnderBeslutning, søknad, rammevedtakSøknadsbehandling, meldekortbehandlingUnderBeslutning) = iverksettSøknadsbehandlingOgBeslutterTarBehandling(
            tac = tac,
            beslutter = beslutter,
        ) ?: return null
        val (oppdatertSak, oppdatertMeldekort, json) = underkjennMeldekortbehandling(
            tac = tac,
            sakId = sakMedMeldekortbehandlingUnderBeslutning.id,
            meldekortId = meldekortbehandlingUnderBeslutning.id,
            beslutter = beslutter,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
        ) ?: return null
        return Tuple5(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            oppdatertMeldekort,
            json,
        )
    }

    /**
     * Forventer at det allerede finnes en sak og meldekortbehandling i status UNDER_BESLUTNING
     */
    suspend fun ApplicationTestBuilder.underkjennMeldekortbehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        beslutter: Saksbehandler = ObjectMother.beslutter("beslutter"),
        begrunnelse: String = "begrunnelse for underkjennelse",
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, MeldekortUnderBehandling, MeldeperiodeKjedeDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = beslutter,
        )
        tac.leggTilBruker(jwt, beslutter)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/meldekort/$meldekortId/underkjenn")
            },
            jwt = jwt,
        ) {
            setBody("""{"begrunnelse": "$begrunnelse"}""")
        }.apply {
            val bodyAsText = bodyAsText()
            withClue(
                "Response details:\n" +
                    "Status: ${this.status}\n" +
                    "Content-Type: ${this.contentType()}\n" +
                    "Body: $bodyAsText}\n",
            ) {
                if (forventetStatus != null) status shouldBe forventetStatus
                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                if (forventetJsonBody != null) bodyAsText.shouldEqualJson(forventetJsonBody)
            }
            if (status != HttpStatusCode.OK) return null
            val jsonObject: MeldeperiodeKjedeDTOJson = JSONObject(bodyAsText)
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            val meldekortbehandling = oppdatertSak.hentMeldekortBehandling(meldekortId) as MeldekortUnderBehandling
            meldekortbehandling.status shouldBe MeldekortBehandlingStatus.UNDER_BEHANDLING
            meldekortbehandling.attesteringer.single().also {
                it.beslutter shouldBe beslutter.navIdent
                it.begrunnelse!!.value shouldBe begrunnelse
                it.status shouldBe Attesteringsstatus.SENDT_TILBAKE
            }
            return Triple(
                oppdatertSak,
                meldekortbehandling,
                jsonObject,
            )
        }
    }
}
