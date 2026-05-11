package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.motta

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
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.meldekort.BrukerutfyltMeldekortDTO
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.mottaMeldekortRoute]
 */
interface MottaMeldekortBuilder {

    fun Meldeperiode.tilUtfyltFraBruker(): Map<LocalDate, BrukerutfyltMeldekortDTO.Status> {
        return this.girRett.entries.associate { (dato, harRett) ->
            dato to (if (harRett) BrukerutfyltMeldekortDTO.Status.DELTATT_UTEN_LØNN_I_TILTAKET else BrukerutfyltMeldekortDTO.Status.IKKE_BESVART)
        }
    }

    suspend fun ApplicationTestBuilder.mottaMeldekortRequest(
        tac: TestApplicationContext,
        meldeperiodeId: MeldeperiodeId,
        sakId: SakId,
        id: MeldekortId = MeldekortId.random(),
        dager: Map<LocalDate, BrukerutfyltMeldekortDTO.Status>,
        mottatt: LocalDateTime = nå(tac.clock),
        journalpostId: String = "1234",
        forventetStatus: HttpStatusCode = HttpStatusCode.OK,
        medJsonBody: ((jsonBody: String) -> Unit)? = null,
    ): Triple<Sak, BrukersMeldekort?, String> {
        val jwt = tac.jwtGenerator.createJwtForSystembruker(
            roles = listOf("lagre_meldekort"),
        )
        tac.leggTilBruker(jwt, ObjectMother.systembrukerLagreMeldekort())

        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/meldekort/motta")
            },
            jwt = jwt,
        ) {
            val dagerJson = dager.entries.joinToString(separator = ",\n") { (dato, status) ->
                """    "$dato": "$status""""
            }

            @Language("JSON")
            val body = """
                {
                  "id": "$id",
                  "meldeperiodeId": "$meldeperiodeId",
                  "sakId": "$sakId",
                  "mottatt": "$mottatt",
                  "journalpostId": "$journalpostId",
                  "dager": { $dagerJson }
                }
            """.trimIndent()

            setBody(body)
        }.apply {
            val bodyAsText = this.bodyAsText()

            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe forventetStatus

                if (medJsonBody != null) {
                    medJsonBody(bodyAsText)
                }
            }

            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            val brukersMeldekort = tac.meldekortContext.brukersMeldekortRepo.hentForMeldekortId(id)

            return Triple(
                oppdatertSak,
                brukersMeldekort,
                bodyAsText,
            )
        }
    }
}
