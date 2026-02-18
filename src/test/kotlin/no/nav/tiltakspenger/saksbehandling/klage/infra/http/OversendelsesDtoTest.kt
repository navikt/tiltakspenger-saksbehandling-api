package no.nav.tiltakspenger.saksbehandling.klage.infra.http

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class OversendelsesDtoTest {

    @Test
    fun `mapper klagebehandling til oversendelseDto`() {
        val journalpostIdVedtak = JournalpostId("vedtak-journalpost-id")
        val klagebehandling = ObjectMother.opprettholdtKlagebehandlingKlarForOversendelse(
            innstillingsbrevJournalpostId = JournalpostId("innstillingsbrev-journalpost-id"),
        )
        val fnr = klagebehandling.fnr
        val saksnummer = klagebehandling.saksnummer
        val klagebehandlingId = klagebehandling.id
        val journalpostIdKlage = klagebehandling.klagensJournalpostId

        klagebehandling.toOversendelsesDto(journalpostIdVedtak) shouldBe KlageOversendelseDto(
            sakenGjelder = SakenGjelder(id = SakenGjelderId(fnr.verdi)),
            fagsak = OversendelsesFagsak(fagsakId = saksnummer.verdi),
            kildeReferanse = klagebehandlingId.toString(),
            dvhReferanse = klagebehandlingId.toString(),
            hjemler = listOf("FS_TIP_3"),
            tilknyttedeJournalposter = listOf(
                TilknyttetJournalpost(
                    type = TilknyttetJournalpostType.BRUKERS_KLAGE,
                    journalpostId = journalpostIdKlage.toString(),
                ),
                TilknyttetJournalpost(
                    type = TilknyttetJournalpostType.OPPRINNELIG_VEDTAK,
                    journalpostId = journalpostIdVedtak.toString(),
                ),
                TilknyttetJournalpost(
                    type = TilknyttetJournalpostType.OVERSENDELSESBREV,
                    journalpostId = "innstillingsbrev-journalpost-id",
                ),
            ),
            brukersKlageMottattVedtaksinstans = 16.februar(2026),
        )
    }
}
