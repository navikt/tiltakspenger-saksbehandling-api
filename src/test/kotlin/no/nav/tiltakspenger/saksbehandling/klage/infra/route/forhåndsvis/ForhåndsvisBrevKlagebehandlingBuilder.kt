package no.nav.tiltakspenger.saksbehandling.klage.infra.route.forhåndsvis

import arrow.core.Tuple4
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettMeldekortVedtakOgOppdaterKlagebehandlingTilAvvisningBrevtekst
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.brev.forhåndsvisBrevKlagebehandlingRoute]
 */
interface ForhåndsvisBrevKlagebehandlingBuilder {
    /** 1. Oppretter ny sak
     *  2. Starter klagebehandling til avvisning
     *  3. Genererer vedtaksbrev for forhåndsvisning
     */
    suspend fun ApplicationTestBuilder.opprettSakOgForhåndsvisKlagebehandlingTilAvvisningsbrev(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
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
                forventetContenttype = ContentType.parse("multipart/mixed; boundary=pdf-boundary"),
                forventetPdf = forventetPdf,
            )!!,
        )
    }

    suspend fun ApplicationTestBuilder.iverksettMeldekortvedtakOgForhåndsvisKlagebehandlingTilAvvisningsbrev(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetPdf: PdfA? = null,
    ): Tuple4<Sak, Meldekortvedtak, Klagebehandling, PdfA>? {
        val (sak, meldekortvedtak, klagebehandling, _) = this.iverksettMeldekortVedtakOgOppdaterKlagebehandlingTilAvvisningBrevtekst(
            tac = tac,
            saksbehandler = saksbehandler,
            fnr = fnr,
        ) ?: return null
        return Tuple4(
            sak,
            meldekortvedtak,
            klagebehandling,
            forhåndsvisKlagebehandlingsbrev(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = saksbehandler,
                forventetStatus = forventetStatus,
                forventetContenttype = ContentType.parse("multipart/mixed; boundary=pdf-boundary"),
                forventetPdf = forventetPdf,
            )!!,
        )
    }

    /** Forventer at det allerede finnes en sak. */
    suspend fun ApplicationTestBuilder.forhåndsvisKlagebehandlingsbrev(
        tac: TestApplicationContext,
        sakId: SakId,
        klagebehandlingId: KlagebehandlingId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        tekstTilVedtaksbrev: List<TittelOgTekst> = listOf(
            TittelOgTekst(
                tittel = NonBlankString.create("Avvisning av klage"),
                tekst = NonBlankString.create("Din klage er dessverre avvist."),
            ),
        ),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetContenttype: ContentType? = ContentType.parse("multipart/mixed; boundary=pdf-boundary"),
        forventetPdf: PdfA? = null,
    ): PdfA? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        val response = defaultRequestWithAssertions(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/klage/$klagebehandlingId/forhandsvis")
            },
            jwt = jwt,
            forventet = forventetStatus?.let { status ->
                ForventetRespons(status = status, contentType = forventetContenttype)
            },
        ) {
            val tekstTilVedtaksbrevListe = tekstTilVedtaksbrev.joinToString(separator = ",") {
                """
                {
                    "tittel": "${it.tittel.value}",
                    "tekst": "${it.tekst.value}"
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
        val pdfBytes = PdfA(response.bodyAsBytes())
        if (forventetPdf != null) pdfBytes shouldBe forventetPdf
        if (response.status != HttpStatusCode.OK) return null
        return pdfBytes
    }
}
