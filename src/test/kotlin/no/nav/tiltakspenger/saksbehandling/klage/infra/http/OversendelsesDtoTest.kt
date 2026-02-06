package no.nav.tiltakspenger.saksbehandling.klage.infra.http

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OversendelsesDtoTest {

    @Test
    fun `mapper klagebehandling til oversendelseDto`() {
        val fnr = Fnr.random()
        val saksnummer = Saksnummer.genererSaknummer(5.februar(2026), "1001")
        val klagebehandlingId = KlagebehandlingId.random()
        val journalpostIdKlage = JournalpostId("klage-journalpost-id")
        val mottattJournalpostDato = LocalDateTime.now(fixedClock)
        val journalpostIdVedtak = JournalpostId("vedtak-journalpost-id")
        val klagebehandling = ObjectMother.opprettKlagebehandling(
            id = klagebehandlingId,
            fnr = fnr,
            saksnummer = saksnummer,
            journalpostId = journalpostIdKlage,
            journalpostOpprettet = mottattJournalpostDato,
            vedtakDetKlagesPå = VedtakId.random(),
            erKlagerPartISaken = true,
            klagesDetPåKonkreteElementerIVedtaket = true,
            erKlagefristenOverholdt = true,
            erUnntakForKlagefrist = null,
            erKlagenSignert = true,
        )

        klagebehandling.toOversendelsesDto(journalpostIdVedtak) shouldBe KlageOversendelseDto(
            sakenGjelder = SakenGjelder(id = SakenGjelderId(fnr.verdi)),
            fagsak = OversendelsesFagsak(fagsakId = saksnummer.verdi),
            kildeReferanse = klagebehandlingId.toString(),
            dvhReferanse = klagebehandlingId.toString(),
            hjemler = listOf(),
            tilknyttedeJournalposter = listOf(
                TilknyttetJournalpost(
                    type = TilknyttetJournalpostType.BRUKERS_KLAGE,
                    journalpostId = journalpostIdKlage.toString(),
                ),
                TilknyttetJournalpost(
                    type = TilknyttetJournalpostType.OPPRINNELIG_VEDTAK,
                    journalpostId = journalpostIdVedtak.toString(),
                ),
            ),
            brukersKlageMottattVedtaksinstans = mottattJournalpostDato.toLocalDate(),
            hindreAutomatiskSvarbrev = null,
        )
    }
}
