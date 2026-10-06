package no.nav.tiltakspenger.saksbehandling.journalnotat.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalnotat.Journalføringsnotat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * Kolonnen `journalføringsnotat` deles av `rammevedtak` og `meldekortvedtak`, så formatet pinnes her.
 * Endres feltnavn eller tidsformat uten migrering, smeller lesestien på gammel data.
 */
class JournalføringsnotatDbJsonTest {

    private val journalføringsnotat = Journalføringsnotat(
        journalpostId = JournalpostId("467010363"),
        journalføringstidspunkt = LocalDateTime.of(2025, 5, 1, 12, 30, 15, 123456000),
    )

    private val lagretJson = """{"journalpostId":"467010363","journalføringstidspunkt":"2025-05-01T12:30:15.123456"}"""

    @Test
    fun `lagres med journalpostId og journalføringstidspunkt`() {
        journalføringsnotat.toDbJson().shouldEqualJson(lagretJson)
    }

    @Test
    fun `leses tilbake fra lagret json`() {
        lagretJson.toJournalføringsnotat() shouldBe journalføringsnotat
    }
}
