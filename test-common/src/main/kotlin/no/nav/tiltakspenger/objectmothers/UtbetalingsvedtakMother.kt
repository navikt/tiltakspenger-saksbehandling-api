package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.utbetaling.domene.Utbetalingsvedtak
import java.time.LocalDate
import java.time.LocalDateTime

interface UtbetalingsvedtakMother {

    fun utbetalingsvedtak(
        id: VedtakId = VedtakId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(LocalDate.now(), "1001"),
        fnr: Fnr = Fnr.random(),
        rammevedtakId: VedtakId = VedtakId.random(),
        periode: Periode = Periode(2.januar(2023), 15.januar(2023)),
        meldekort: MeldekortBehandling.UtfyltMeldekort = ObjectMother.utfyltMeldekort(
            sakId = sakId,
            rammevedtakId = rammevedtakId,
            fnr = fnr,
            periode = periode,
        ),
        forrigeUtbetalingsvedtakId: VedtakId? = null,
        sendtTilUtbetaling: LocalDateTime? = null,
        journalpostId: JournalpostId? = null,
        journalføringstidspunkt: LocalDateTime? = null,
        opprettet: LocalDateTime = nå(),
    ): Utbetalingsvedtak {
        return Utbetalingsvedtak(
            id = id,
            opprettet = opprettet,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            rammevedtakId = rammevedtakId,
            meldekortbehandling = meldekort,
            forrigeUtbetalingsvedtakId = forrigeUtbetalingsvedtakId,
            sendtTilUtbetaling = sendtTilUtbetaling,
            journalpostId = journalpostId,
            journalføringstidspunkt = journalføringstidspunkt,
        )
    }
}
