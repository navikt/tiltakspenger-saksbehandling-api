package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.setBody
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
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvbrytMeldekortBehandlingRouteTest {
    @Test
    fun `saksbehandler kan avbryte meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val saksbehandler = ObjectMother.saksbehandler()
            val (sak, _, _, meldekortUnderBehandling, _) = this.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(
                tac = tac,
                saksbehandler = saksbehandler,
            )!!
            avbrytMeldekortBehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortUnderBehandling.id,
                begrunnelse = "begrunnelse",
                saksbehandler = saksbehandler,
            ).also {
                val oppdatertMeldekortbehandling =
                    tac.meldekortContext.meldekortBehandlingRepo.hent(meldekortUnderBehandling.id)
                oppdatertMeldekortbehandling shouldNotBe null
                oppdatertMeldekortbehandling?.status shouldBe MeldekortBehandlingStatus.AVBRUTT
                oppdatertMeldekortbehandling?.avbrutt?.saksbehandler shouldBe saksbehandler.navIdent
                oppdatertMeldekortbehandling?.avbrutt?.tidspunkt?.toLocalDate() shouldBe 1.januar(2025)
                oppdatertMeldekortbehandling?.avbrutt?.begrunnelse shouldBe "begrunnelse"

                val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
                oppdatertSak.meldekortbehandlinger.ikkeAvbrutteMeldekortBehandlinger shouldBe emptyList()
                oppdatertSak.meldekortbehandlinger.avbrutteMeldekortBehandlinger.size shouldBe 1
            }
        }
    }

    private suspend fun ApplicationTestBuilder.avbrytMeldekortBehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        begrunnelse: String,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): String {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/meldekort/$meldekortId/avbryt")
            },
            jwt = jwt,
        ) {
            this.setBody("""{"begrunnelse":"$begrunnelse"}""")
        }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe HttpStatusCode.OK
            }
            return bodyAsText
        }
    }
}
