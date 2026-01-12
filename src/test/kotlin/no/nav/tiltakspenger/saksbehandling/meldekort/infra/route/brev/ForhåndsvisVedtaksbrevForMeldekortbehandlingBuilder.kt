package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.brev

import arrow.core.Tuple5
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
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.buildDagerBody
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.forhåndsvisBrevMeldekortbehandling]
 * Se også [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.oppdater.OppdaterMeldekortbehandlingBuilder]
 */
interface ForhåndsvisVedtaksbrevForMeldekortbehandlingBuilder {
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgForhåndsvisVedtaksbrevForMeldekortbehandling(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
        tekstTilVedtaksbrev: String? = "Dette er et vedtaksbrev",
        dager: List<Pair<LocalDate, MeldekortDagStatus>>? = null,
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetPdf: PdfA? = null,
    ): Tuple5<Sak, Søknad, Rammevedtak, MeldekortUnderBehandling, PdfA>? {
        val (_, søknad, rammevedtakSøknadsbehandling, opprettetMeldekortbehandling) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(
            tac = tac,
        ) ?: return null
        val (oppdatertSak, oppdatertMeldekortbehandling, jsonResponse) = forhåndsvisVedtaksbrevForMeldekortbehandling(
            tac = tac,
            sakId = opprettetMeldekortbehandling.sakId,
            meldekortId = opprettetMeldekortbehandling.id,
            saksbehandler = saksbehandler,
            tekstTilVedtaksbrev = tekstTilVedtaksbrev,
            dager = dager,
            forventetStatus = forventetStatus,
            forventetPdf = forventetPdf,
        ) ?: return null
        return Tuple5(
            oppdatertSak,
            søknad,
            rammevedtakSøknadsbehandling,
            oppdatertMeldekortbehandling,
            jsonResponse,
        )
    }

    /**
     * Forventer at det allerede finnes en sak og meldekortbehandling.
     */
    suspend fun ApplicationTestBuilder.forhåndsvisVedtaksbrevForMeldekortbehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
        tekstTilVedtaksbrev: String? = "Dette er et vedtaksbrev",
        dager: List<Pair<LocalDate, MeldekortDagStatus>>? = null,
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetPdf: PdfA? = null,
    ): Triple<Sak, MeldekortUnderBehandling, PdfA>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        val dagerIBody = buildDagerBody(tac = tac, sakId = sakId, meldekortId = meldekortId, dager = dager)
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/sak/$sakId/meldekortbehandling/$meldekortId/forhandsvis")
            },
            jwt = jwt,
        ) {
            this.setBody(
                """
                    {
                    "tekstTilVedtaksbrev":${if (tekstTilVedtaksbrev != null) "\"$tekstTilVedtaksbrev\"" else null},
                    "dager":$dagerIBody
                    }
                """.trimIndent(),
            )
        }.apply {
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
            val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
            val meldekortbehandling = oppdatertSak.hentMeldekortBehandling(meldekortId) as MeldekortUnderBehandling
            meldekortbehandling.status shouldBe MeldekortBehandlingStatus.UNDER_BEHANDLING

            return Triple(
                oppdatertSak,
                meldekortbehandling,
                pdfBytes,
            )
        }
    }
}
