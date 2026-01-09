package no.nav.tiltakspenger.saksbehandling.journalpost.infra.route

import com.fasterxml.jackson.module.kotlin.readValue
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
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class ValiderJournalpostRouteTest {
    @Test
    fun `valider - journalpost finnes og gjelder innsendt bruker`() {
        withTestApplicationContext { tac ->
            val fnr = Fnr.random()
            val journalpostId = JournalpostId("1234567")
            tac.safJournalpostFakeClient.addJournalpost(journalpostId, fnr)
            validerJournalpost(tac, fnr, journalpostId).also {
                val response = objectMapper.readValue<ValiderJournalpostResponse>(it)
                response.journalpostFinnes shouldBe true
                response.gjelderInnsendtFnr shouldBe true
            }
        }
    }

    @Test
    fun `valider - journalpost finnes og gjelder annen bruker`() {
        withTestApplicationContext { tac ->
            val fnr = Fnr.random()
            val journalpostId = JournalpostId("12345678")
            tac.safJournalpostFakeClient.addJournalpost(journalpostId, Fnr.random())
            validerJournalpost(tac, fnr, journalpostId).also {
                val response = objectMapper.readValue<ValiderJournalpostResponse>(it)
                response.journalpostFinnes shouldBe true
                response.gjelderInnsendtFnr shouldBe false
            }
        }
    }

    @Test
    fun `valider - journalpost finnes ikke`() {
        withTestApplicationContext { tac ->
            val fnr = Fnr.random()
            val journalpostId = JournalpostId("12345679")
            validerJournalpost(tac, fnr, journalpostId).also {
                val response = objectMapper.readValue<ValiderJournalpostResponse>(it)
                response.journalpostFinnes shouldBe false
                response.gjelderInnsendtFnr shouldBe null
            }
        }
    }

    private suspend fun ApplicationTestBuilder.validerJournalpost(
        tac: TestApplicationContext,
        fnr: Fnr,
        journalpostId: JournalpostId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
    ): String {
        tac.tilgangsmaskinFakeClient.leggTil(fnr, Tilgangsvurdering.Godkjent)
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/journalpost/valider")
            },
            jwt = jwt,
        ) {
            setBody(
                serialize(ValiderJournalpostBody(fnr.verdi, journalpostId.toString())),
            )
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
