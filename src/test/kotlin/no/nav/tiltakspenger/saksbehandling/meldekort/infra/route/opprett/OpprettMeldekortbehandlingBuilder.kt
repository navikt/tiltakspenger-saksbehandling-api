package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.opprett

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
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.MeldeperiodeKjedeDTOJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.json.JSONObject
import java.net.URLEncoder
import java.net.URLEncoder.encode

interface OpprettMeldekortbehandlingBuilder {

    /** Forventer at det allerede finnes en sak, søknad og iverksatt søknadsbehandling m/rammevedtak */
    suspend fun ApplicationTestBuilder.opprettMeldekortbehandlingForSakId(
        tac: TestApplicationContext,
        sakId: SakId,
        kjedeId: MeldeperiodeKjedeId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: String? = null,
    ): Triple<Sak, MeldekortUnderBehandling, MeldeperiodeKjedeDTOJson>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        val kjedeId = "${kjedeId.fraOgMed}%2F${kjedeId.tilOgMed}"
        defaultRequest(
            HttpMethod.Post,
            "/sak/$sakId/meldeperiode/$kjedeId/opprettBehandling",
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
            val jsonObject: MeldeperiodeKjedeDTOJson = JSONObject(bodyAsText)
            val meldekortbehandlingerJson = jsonObject.getJSONArray("meldekortBehandlinger")
            val meldekortbehandlingJson =
                meldekortbehandlingerJson.getJSONObject(meldekortbehandlingerJson.length() - 1)
            val meldekortbehandlingId = MeldekortId.fromString(meldekortbehandlingJson.getString("id"))
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            return Triple(
                oppdatertSak,
                tac.meldekortContext.meldekortBehandlingRepo.hent(meldekortId = meldekortbehandlingId) as MeldekortUnderBehandling,
                jsonObject,
            )
        }
    }
}
