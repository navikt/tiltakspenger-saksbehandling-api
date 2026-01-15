package no.nav.tiltakspenger.saksbehandling.klage.infra.route.forhåndsvis

import io.kotest.assertions.json.CompareJsonOptions
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.forhåndsvisBrevKlagebehandlingRoute]
 */
interface ForhåndsvisBrevKlagebehandlingBuilder {
    /** 1. Oppretter ny sak
     *  2. Starter klagebehandling til avvisning
     *  3. Oppdaterer formkrav
     */
    suspend fun ApplicationTestBuilder.opprettSakOgForhåndsvisKlagebehandlingsbrev(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetPdf: PdfA? = null,
    ): Triple<Sak, Klagebehandling, PdfA>? {
        val (sak, klagebehandling, _) = this.opprettSakOgKlagebehandlingTilAvvisning(
            tac = tac,
            saksbehandler = saksbehandler,
            fnr = fnr,
        ) ?: return null
        return Triple(
            sak,
            klagebehandling,
            forhåndsvisKlagebehandlingsbrev(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = saksbehandler,
                forventetStatus = forventetStatus,
                forventetPdf = forventetPdf,
            )!!,
        )
    }

    /** Forventer at det allerede finnes en sak. */
    suspend fun ApplicationTestBuilder.forhåndsvisKlagebehandlingsbrev(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        tekstTilVedtaksbrev: List<TittelOgTekst> = listOf(
            TittelOgTekst(
                tittel = NonBlankString.create("Avvisning av klage"),
                tekst = NonBlankString.create("Din klage er dessverre avvist."),
            ),
        ),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetPdf: PdfA? = null,
    ): PdfA? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/klage/$klagebehandlingId/forhandsvis")
            },
            jwt = jwt,
        ) {
            val tekstTilVedtaksbrevListe = tekstTilVedtaksbrev.joinToString(separator = ",") {
                """
                {
                    "tittel": "${it.tittel}",
                    "tekst": "${it.tekst}"
                }
                """.trimIndent()
            }
            setBody(
                //language=JSON
                """
                {
                    "tekstTilVedtaksbrev": [$tekstTilVedtaksbrevListe]
                }
                """.trimIndent(),
            )
        }
            .apply {
                val pdfBytes = PdfA(bodyAsBytes())
                withClue(
                    "Response details:\n" +
                        "Status: ${this.status}\n" +
                        "Content-Type: ${this.contentType()}\n",
                ) {
                    if (forventetStatus != null) status shouldBe forventetStatus
                    contentType() shouldBe ContentType.parse("application/pdf")
                    if (forventetPdf != null) pdfBytes shouldBe forventetPdf
                }
                if (status != HttpStatusCode.OK) return null
                return pdfBytes
            }
    }
}
