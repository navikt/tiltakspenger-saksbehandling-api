package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

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
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterBehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.medQuotes
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.intellij.lang.annotations.Language
import java.time.LocalDate

interface OppdaterBehandlingBuilder {

    suspend fun ApplicationTestBuilder.oppdaterBehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        oppdaterBehandlingDTO: OppdaterBehandlingDTO,
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Triple<Sak, Rammebehandling, String> {
        return oppdaterBehandling(
            tac = tac,
            sakId = sakId,
            behandlingId = behandlingId,
            body = serialize(oppdaterBehandlingDTO),
            forventetStatus = forventetStatus,
            saksbehandler = saksbehandler,
        )
    }

    // TODO: lag en funksjon for hver behandlingstype
    suspend fun ApplicationTestBuilder.oppdaterRevurderingStans(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        begrunnelseVilkårsvurdering: String? = null,
        fritekstTilVedtaksbrev: String? = null,
        harValgtStansFraFørsteDagSomGirRett: Boolean = true,
        stansFraOgMed: LocalDate? = null,
        valgteHjemler: Set<ValgtHjemmelForStans> = setOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Triple<Sak, Rammebehandling, String> {
        @Language("JSON")
        val body = """
            {
              "begrunnelseVilkårsvurdering": ${begrunnelseVilkårsvurdering?.medQuotes()},
              "fritekstTilVedtaksbrev": ${fritekstTilVedtaksbrev?.medQuotes()},
              "valgteHjemler": [${valgteHjemler.joinToString(",") { it.tilDTO().toString().medQuotes() }}],
              "harValgtStansFraFørsteDagSomGirRett": $harValgtStansFraFørsteDagSomGirRett,
              "stansFraOgMed": ${stansFraOgMed?.toString()?.medQuotes()},
              "resultat": "STANS"
            }
        """.trimIndent()

        return oppdaterBehandling(
            tac = tac,
            sakId = sakId,
            behandlingId = behandlingId,
            body = body,
            forventetStatus = forventetStatus,
            saksbehandler = saksbehandler,
        )
    }

    suspend fun ApplicationTestBuilder.oppdaterBehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: BehandlingId,
        body: String,
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): Triple<Sak, Rammebehandling, String> {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/behandling/$behandlingId/oppdater")
            },
            jwt = jwt,
        ) {
            setBody(body)
        }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe forventetStatus
            }
            val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            val behandling = tac.behandlingContext.behandlingRepo.hent(behandlingId)
            return Triple(sak, behandling, bodyAsText)
        }
    }
}
