package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import arrow.core.left
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
import io.ktor.server.util.url
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasIntrospectionResponse
import no.nav.tiltakspenger.saksbehandling.auditlog.AuditService
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeOppdatereMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.KanIkkeSendeMeldekortTilBeslutter
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SendMeldekortTilBeslutterService
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class SendMeldekortBehandlingTilBeslutterRouteTest {

    // TODO jah: Denne kommer til å smelle i 2030. Håper noen fikser denne før det. I tillegg bør den ikke kalle objectmother men kun bruke endepunkter. Gjelder alle tester for meldekort.
    @Test
    fun `meldekortperioden kan ikke være frem i tid`() {
        runTest {
            withTestApplicationContext(clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))) { tac ->
                val (sak, _, _) = this.iverksettSøknadsbehandling(
                    tac = tac,
                    vedtaksperiode = 1.januar(2030) til 31.januar(2030),
                )
                val sakId = sak.id
                val saksbehandler = ObjectMother.saksbehandler123()
                val meldeperiode = sak.meldeperiodeKjeder.first().first()
                val meldekortBehandling = ObjectMother.meldekortUnderBehandling(
                    meldeperiode = meldeperiode,
                    opprettet = LocalDateTime.now(tac.clock),
                    saksbehandler = saksbehandler.navIdent,
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                    fnr = sak.fnr,
                    status = MeldekortBehandlingStatus.UNDER_BEHANDLING,
                )

                tac.meldekortContext.meldekortBehandlingRepo.lagre(meldekortBehandling, null)
                val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
                    saksbehandler = saksbehandler,
                )
                tac.leggTilBruker(jwt, saksbehandler)
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("/sak/$sakId/meldekort/${meldekortBehandling.id}")
                    },
                    jwt = jwt,
                ) {
                    setBody("""{"dager": [{"dato":"2030-01-01","status":"FRAVÆR_SYK"}]}""")
                }.apply {
                    withClue(
                        "Response details:\n" +
                            "Status: ${this.status}\n" +
                            "Content-Type: ${this.contentType()}\n" +
                            "Body: ${this.bodyAsText()}\n",
                    ) {
                        status shouldBe HttpStatusCode.BadRequest
                        contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                        bodyAsText() shouldBe """{"melding":"Kan ikke sende inn et meldekort før meldekortperioden har begynt.","kode":"meldekortperioden_kan_ikke_være_frem_i_tid"}"""
                    }
                }
            }
        }
    }
}
