package no.nav.tiltakspenger.vedtak.routes

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.objectmothers.førstegangsbehandlingIverksatt
import no.nav.tiltakspenger.vedtak.routes.behandling.benk.startRevurderingRoute
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.tiltakdeltagelse.tiltakDeltagelseRoutes
import org.junit.jupiter.api.Test

internal class StartRevurderingTest {

    @Test
    fun `kan endre tiltaksdeltagelsesvilkår`() {
        runTest {
            with(TestApplicationContext()) {
                val beslutter = beslutter()
                val saksbehandler = saksbehandler()
                val tac = this
                val sak = this.førstegangsbehandlingIverksatt(saksbehandler = saksbehandler, beslutter = beslutter)
                val revurderingsperiode = Periode(
                    sak.førstegangsbehandling.vurderingsperiode.fraOgMed.plusMonths(1),
                    sak.førstegangsbehandling.vurderingsperiode.tilOgMed,
                )
                testApplication {
                    // TODO jah: Vi trenger en generell måte å spinne opp alle routes med fakes på.
                    application {
                        jacksonSerialization()
                        routing {
                            startRevurderingRoute(
                                tokenService = tac.tokenService,
                                startRevurderingService = tac.behandlingContext.startRevurderingService,
                                auditService = tac.personContext.auditService,
                            )
                            tiltakDeltagelseRoutes(
                                tiltaksdeltagelseVilkårService = tac.behandlingContext.tiltaksdeltagelseVilkårService,
                                tokenService = tac.tokenService,
                                auditService = tac.personContext.auditService,
                                behandlingService = tac.behandlingContext.behandlingService,
                            )
                        }
                    }
                    val revurderingId = startRevurdering(sak.id, tac, revurderingsperiode, saksbehandler)
                    oppdaterStatus(sak.id, revurderingId, tac, revurderingsperiode, saksbehandler)
                }
            }
        }
    }

    private suspend fun ApplicationTestBuilder.startRevurdering(
        sakId: SakId,
        tac: TestApplicationContext,
        revurderingsperiode: Periode,
        saksbehandler: Saksbehandler,
    ): BehandlingId {
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/revurdering")
            },
            jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler),
        ) {
            setBody(
                """
                {
                  "periode": {
                    "fraOgMed": "${revurderingsperiode.fraOgMed}",
                    "tilOgMed": "${revurderingsperiode.tilOgMed}"
                  }
                }
                """.trimIndent(),
            )
        }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe HttpStatusCode.OK
                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
            }
            // Ikke så mye og asserte på her
            bodyAsText.shouldContain("\"id\":\"beh_")
            return BehandlingId.fromString(deserialize<StartRevurderingResponseJson>(bodyAsText).id)
        }
    }

    private data class StartRevurderingResponseJson(val id: String)

    private suspend fun ApplicationTestBuilder.oppdaterStatus(
        sakId: SakId,
        revurderingId: BehandlingId,
        tac: TestApplicationContext,
        revurderingsperiode: Periode,
        saksbehandler: Saksbehandler,
    ) {
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/behandling/$revurderingId/vilkar/tiltaksdeltagelse")
            },
            jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler),
        ) {
            setBody(
                """
               {
               "statusForPeriode": [
                {
                  "periode": {
                    "fraOgMed": "${revurderingsperiode.fraOgMed}",
                    "tilOgMed": "${revurderingsperiode.tilOgMed}"
                  },
                  "status": "HarSluttet"
                }
               ],
               "årsakTilEndring": "ENDRING_ETTER_SØKNADSTIDSPUNKT"
               }
                """.trimIndent(),
            )
        }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe HttpStatusCode.OK
                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
            }
            bodyAsText.shouldEqualJson(
                """
{
  "registerSaksopplysning":{
    "tiltakNavn":"Arbeidsmarkedsoppfølging gruppe",
    "deltagelsePeriode":{
      "fraOgMed":"2023-02-01",
      "tilOgMed":"2023-03-31"
    },
    "status":"Deltar",
    "kilde":"KOMET"
  },
  "vilkårLovreferanse":{
    "lovverk":"Tiltakspengeforskriften",
    "paragraf":"§2",
    "beskrivelse":"Hvem som kan få tiltakspenger"
  },
  "utfallperiode":{
    "fraOgMed":"2023-02-01",
    "tilOgMed":"2023-03-31"
  },
  "samletUtfall":"OPPFYLT"
}
                """.trimIndent(),
            )
        }
    }
}
