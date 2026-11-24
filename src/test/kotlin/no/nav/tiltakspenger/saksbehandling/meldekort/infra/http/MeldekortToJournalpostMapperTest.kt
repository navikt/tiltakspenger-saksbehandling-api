package no.nav.tiltakspenger.saksbehandling.meldekort.infra.http

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.dokument.PdfOgJson
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldeperiodebehandlingType
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class MeldekortToJournalpostMapperTest {

    private val pdfOgJson = PdfOgJson(PdfA("pdf".toByteArray()), "{}")

    @Test
    fun `vedtaksbrev som ikke skal distribueres journalføres med kanal INGEN_DISTRIBUSJON`() {
        val vedtak = ObjectMother.meldekortvedtak()

        val request = objectMapper.readTree(vedtak.toJournalpostRequest(pdfOgJson))

        request.get("kanal").asText() shouldBe "INGEN_DISTRIBUSJON"
    }

    @Test
    fun `vedtaksbrev som skal distribueres journalføres med kanal NAV_NO`() {
        val sakId = SakId.random()
        val vedtak = ObjectMother.meldekortvedtak(
            sakId = sakId,
            meldekortbehandling = ObjectMother.meldekortBehandletManuelt(
                sakId = sakId,
                type = MeldeperiodebehandlingType.KORRIGERING,
                skalDistribuereVedtaksbrev = true,
            ),
        )

        val request = objectMapper.readTree(vedtak.toJournalpostRequest(pdfOgJson))

        request.get("kanal").asText() shouldBe "NAV_NO"
    }
}
