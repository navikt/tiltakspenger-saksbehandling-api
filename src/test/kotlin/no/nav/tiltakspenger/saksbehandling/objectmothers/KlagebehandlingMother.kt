package no.nav.tiltakspenger.saksbehandling.objectmothers

import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.opprett.OpprettKlagebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.Clock
import java.time.LocalDateTime

interface KlagebehandlingMother : MotherOfAllMothers {
    fun opprettKlagebehandling(
        clock: Clock = this.clock,
        id: KlagebehandlingId = KlagebehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        opprettet: LocalDateTime = LocalDateTime.now(clock),
        journalpostOpprettet: LocalDateTime = LocalDateTime.now(clock),
        journalpostId: JournalpostId = JournalpostId("journalpostId"),
        vedtakDetKlagesPå: VedtakId? = null,
        erKlagerPartISaken: Boolean = true,
        klagesDetPåKonkreteElementerIVedtaket: Boolean = true,
        erKlagefristenOverholdt: Boolean = true,
        erKlagenSignert: Boolean = true,
        correlationId: CorrelationId = CorrelationId.generate(),
    ): Klagebehandling {
        return runBlocking {
            Klagebehandling.opprett(
                id = id,
                saksnummer = saksnummer,
                fnr = fnr,
                opprettet = opprettet,
                journalpostOpprettet = journalpostOpprettet,
                kommando = OpprettKlagebehandlingKommando(
                    sakId = sakId,
                    saksbehandler = saksbehandler,
                    journalpostId = journalpostId,
                    vedtakDetKlagesPå = vedtakDetKlagesPå,
                    erKlagerPartISaken = erKlagerPartISaken,
                    klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
                    erKlagefristenOverholdt = erKlagefristenOverholdt,
                    erKlagenSignert = erKlagenSignert,
                    correlationId = correlationId,
                ),
            )
        }
    }
}
