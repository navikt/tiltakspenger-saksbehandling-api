package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.brev

import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.felles.erHelg
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgForhåndsvisVedtaksbrevForMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import org.junit.jupiter.api.Test

class ForhåndsvisVedtaksbrevForMeldekortbehandlingTest {
    @Test
    fun `kan forhåndsvise vedtaksbrev`() {
        withTestApplicationContext { tac ->
            val (_, _, _, _, _) = iverksettSøknadsbehandlingOgForhåndsvisVedtaksbrevForMeldekortbehandling(
                tac = tac,
            )!!
        }
    }

    @Test
    fun `kan forhåndsvise vedtaksbrev med legacy body`() {
        withTestApplicationContext { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandler")
            val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
            tac.leggTilBruker(jwt, saksbehandler)

            val (sak, _, _, opprettetMeldekortbehandling, _) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(
                tac = tac,
                saksbehandler = saksbehandler,
            )!!

            val dagerJson = opprettetMeldekortbehandling.dagerLegacy.map { dag ->
                val status = when {
                    dag.status == MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
                    dag.status == MeldekortDagStatus.IKKE_BESVART && dag.dato.erHelg() -> MeldekortDagStatus.IKKE_TILTAKSDAG
                    dag.status == MeldekortDagStatus.IKKE_BESVART -> MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
                    else -> dag.status
                }
                dag.dato to status
            }
                .joinToString(prefix = "[", postfix = "]", separator = ",") { (dato, status) ->
                    """
                        {
                            "dato":"$dato",
                            "status":"$status"
                        }
                    """.trimIndent()
                }

            defaultRequest(
                HttpMethod.Post,
                url {
                    protocol = URLProtocol.HTTPS
                    path("/sak/${sak.id}/meldekortbehandling/${opprettetMeldekortbehandling.id}/forhandsvis")
                },
                jwt = jwt,
            ) {
                setBody(
                    """
                        {
                        "versjon":1,
                        "tekstTilVedtaksbrev":"Dette er et vedtaksbrev",
                        "dager":$dagerJson
                        }
                    """.trimIndent(),
                )
            }.apply {
                status shouldBe HttpStatusCode.OK
                contentType() shouldBe ContentType.parse("application/pdf")
            }
        }
    }
}
