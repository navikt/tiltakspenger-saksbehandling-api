package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.klage.domene.forvaltningsmelding.KlageForvaltningsmelding
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Pinner den faktiske json-en i `klagebehandling.forvaltningsmelding`, ikke bare rundturen.
 * Jobbspørringene i [KlagebehandlingPostgresRepo] leser `journalpostId`, `distribusjonId` og `sendtTidspunkt` direkte fra json-en, så feltnavnene er en del av kontrakten.
 */
class KlageForvaltningsmeldingDbJsonTest {

    private val brevtekst = Brevtekster(listOf(TittelOgTekst(tittel = "Svartid", tekst = "Ca. 3 måneder.")))

    @Test
    fun `en sendt forvaltningsmelding som ikke er journalført`() {
        val forvaltningsmelding = KlageForvaltningsmelding.ny(
            brevtekst = brevtekst,
            saksbehandler = "Z12345",
            sendtTidspunkt = LocalDateTime.parse("2025-01-01T12:00:00"),
        )
        val lagret = """
            {
              "brevtekst": [{"tittel": "Svartid", "tekst": "Ca. 3 måneder."}],
              "saksbehandler": "Z12345",
              "sendtTidspunkt": "2025-01-01T12:00:00",
              "brevdato": null,
              "journalpostId": null,
              "journalføringstidspunkt": null,
              "distribusjonId": null,
              "distribusjonstidspunkt": null
            }
        """.trimIndent()

        forvaltningsmelding.toDbJson() shouldEqualJson lagret
        lagret.toKlageForvaltningsmelding() shouldBe forvaltningsmelding
    }

    @Test
    fun `en journalført og distribuert forvaltningsmelding`() {
        val forvaltningsmelding = KlageForvaltningsmelding.ny(
            brevtekst = brevtekst,
            saksbehandler = "Z12345",
            sendtTidspunkt = LocalDateTime.parse("2025-01-01T12:00:00"),
        ).oppdaterJournalpost(
            brevdato = LocalDate.parse("2025-01-02"),
            journalpostId = JournalpostId("123"),
            tidspunkt = LocalDateTime.parse("2025-01-02T08:00:00"),
        ).oppdaterDistribusjon(
            distribusjonId = DistribusjonId("456"),
            tidspunkt = LocalDateTime.parse("2025-01-02T09:00:00"),
        )
        val lagret = """
            {
              "brevtekst": [{"tittel": "Svartid", "tekst": "Ca. 3 måneder."}],
              "saksbehandler": "Z12345",
              "sendtTidspunkt": "2025-01-01T12:00:00",
              "brevdato": "2025-01-02",
              "journalpostId": "123",
              "journalføringstidspunkt": "2025-01-02T08:00:00",
              "distribusjonId": "456",
              "distribusjonstidspunkt": "2025-01-02T09:00:00"
            }
        """.trimIndent()

        forvaltningsmelding.toDbJson() shouldEqualJson lagret
        lagret.toKlageForvaltningsmelding() shouldBe forvaltningsmelding
    }
}
