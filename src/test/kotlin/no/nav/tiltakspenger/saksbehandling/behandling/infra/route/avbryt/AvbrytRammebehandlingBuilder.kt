package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.avbryt

import arrow.core.Tuple4
import io.kotest.assertions.json.CompareJsonOptions
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.SakDTOJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.behandling.infra.route.rammebehandlingRoutes]
 */
interface AvbrytRammebehandlingBuilder {
    /**
     * 1. Oppretter ny sak og søknad
     * 2. Starter søknadsbehandling under behandling
     * 3. Avbryter
     */
    suspend fun ApplicationTestBuilder.opprettSøknadsbehandlingOgAvbryt(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Tuple4<Sak, Søknad, Rammebehandling?, SakDTOJson>? {
        val (sak, _, søknadsbehandling) = this.opprettSøknadsbehandlingUnderBehandling(
            tac = tac,
            saksbehandler = saksbehandler,
            fnr = fnr,
        )
        return avbrytRammebehandling(
            tac = tac,
            saksnummer = sak.saksnummer,
            rammebehandlingId = søknadsbehandling.id,
            saksbehandler = saksbehandler,
            forventetStatus = forventetStatus,
            forventetJsonBody = forventetJsonBody,
            sakId = sak.id,
        )
    }

    /**
     * Forventer at det allerede finnes en sak og en åpen rammebehandling under behanding.
     */
    suspend fun ApplicationTestBuilder.avbrytRammebehandling(
        tac: TestApplicationContext,
        saksnummer: Saksnummer,
        sakId: SakId,
        rammebehandlingId: BehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        begrunnelse: String = "begrunnelse for avbryt søknad og/eller rammebehandling",
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
    ): Tuple4<Sak, Søknad, Rammebehandling?, SakDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$saksnummer/avbryt-aktiv-behandling")
            },
            jwt = jwt,
        ) {
            setBody(
                """
                {
                "behandlingId": "$rammebehandlingId",
                "begrunnelse": "$begrunnelse"
                }
                """.trimIndent(),
            )
        }.apply {
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
            val sakJson: SakDTOJson = objectMapper.readTree(bodyAsText)

            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Tuple4(
                oppdatertSak,
                oppdatertSak.søknader.single(),
                rammebehandlingId.let { oppdatertSak.hentRammebehandling(it)!! },
                sakJson,
            )
        }
    }
}
