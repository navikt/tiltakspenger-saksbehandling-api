package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class OppdaterMeldekortbehandlingDTOTest {

    private fun body(vedtaksbrevfelter: String) = """
        {
          "meldeperioder": [{"kjedeId": "2025-03-31/2025-04-13", "dager": [{"dato": "2025-03-31", "status": "DELTATT_UTEN_LØNN_I_TILTAKET"}]}],
          "begrunnelse": null,
          "tekstTilVedtaksbrev": null,
          $vedtaksbrevfelter
          "skalJournalføreNotat": false
        }
    """.trimIndent()

    private fun String.tilKommando() = deserialize<OppdaterMeldekortbehandlingDTO>(this).toDomain(
        saksbehandler = ObjectMother.saksbehandler(),
        meldekortId = MeldekortId.random(),
        sakId = SakId.random(),
        correlationId = CorrelationId.generate(),
    )

    @Test
    fun `leser skalJournalføreVedtaksbrev og skalDistribuereVedtaksbrev`() {
        val kommando = body(""""skalJournalføreVedtaksbrev": true, "skalDistribuereVedtaksbrev": true,""").tilKommando()

        kommando.skalJournalføreVedtaksbrev shouldBe true
        kommando.skalDistribuereVedtaksbrev shouldBe true
    }
}
