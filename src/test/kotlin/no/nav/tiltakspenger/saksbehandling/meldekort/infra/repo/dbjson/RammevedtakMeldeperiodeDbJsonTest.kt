package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * En lagret rad kan ha perioder uten vedtak (`vedtakId: null`), mens dagens skrivesti alltid setter vedtakId.
 * Null-grenen kan dermed bare nås fra lagret json — og mappingen rører ikke postgres.
 */
class RammevedtakMeldeperiodeDbJsonTest {

    @Test
    fun `perioder uten vedtak filtreres bort ved lesing`() {
        val vedtakId = VedtakId.random()

        //language=json
        val lagret = """
        {
          "perioderTilVedtakId": [
            { "periode": { "fraOgMed": "2025-01-06", "tilOgMed": "2025-01-12" }, "vedtakId": null },
            { "periode": { "fraOgMed": "2025-01-13", "tilOgMed": "2025-01-19" }, "vedtakId": "$vedtakId" }
          ]
        }
        """.trimIndent()

        lagret.toPeriodiserteVedtakId().perioderMedVerdi.toList() shouldBe
            listOf(PeriodeMedVerdi(periode = Periode(13.januar(2025), 19.januar(2025)), verdi = vedtakId))
    }
}
