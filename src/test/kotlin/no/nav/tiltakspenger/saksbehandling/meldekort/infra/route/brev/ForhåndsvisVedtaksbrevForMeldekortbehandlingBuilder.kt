package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.brev

import arrow.core.Tuple5
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto.OppdaterMeldekortbehandlingDTO.OppdatertMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.buildMeldeperioderBody
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.forhåndsvisBrevMeldekortbehandlingRoute]
 * Se også [no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.oppdater.OppdaterMeldekortbehandlingBuilder]
 */
interface ForhåndsvisVedtaksbrevForMeldekortbehandlingBuilder {
    suspend fun ApplicationTestBuilder.iverksettSøknadsbehandlingOgForhåndsvisVedtaksbrevForMeldekortbehandling(
        tac: TestApplicationContext,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandler"),
        tekstTilVedtaksbrev: String? = "Dette er et vedtaksbrev",
        meldeperioder: List<OppdatertMeldeperiodeDTO>? = null,
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/pdf"),
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
            meldeperioder = meldeperioder,
            forventet = forventet,
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
        meldeperioder: List<OppdatertMeldeperiodeDTO>? = null,
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/pdf"),
        forventetPdf: PdfA? = null,
    ): Triple<Sak, MeldekortUnderBehandling, PdfA>? {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(
            saksbehandler = saksbehandler,
        )
        tac.leggTilBruker(jwt, saksbehandler)
        val meldeperioderIBody = buildMeldeperioderBody(tac = tac, sakId = sakId, meldekortId = meldekortId, meldeperioder = meldeperioder)
        val response = defaultRequestWithAssertions(
            HttpMethod.POST,
            "/sak/$sakId/meldekortbehandling/$meldekortId/forhandsvis",
            jwt = jwt,
            forventet = forventet,
            body = """
                    {
                    "tekstTilVedtaksbrev":${if (tekstTilVedtaksbrev != null) "\"$tekstTilVedtaksbrev\"" else null},
                    "meldeperioder":$meldeperioderIBody
                    }
            """.trimIndent(),
        )
        val pdfBytes = PdfA(response.bytes)
        if (forventetPdf != null) pdfBytes shouldBe forventetPdf
        if (response.statusCode != 200) return null
        val oppdatertSak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
        val meldekortbehandling = oppdatertSak.hentMeldekortbehandling(meldekortId) as MeldekortUnderBehandling
        meldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING

        return Triple(
            oppdatertSak,
            meldekortbehandling,
            pdfBytes,
        )
    }
}
