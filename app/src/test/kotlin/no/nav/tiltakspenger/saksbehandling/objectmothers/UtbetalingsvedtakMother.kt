package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.felles.januar
import no.nav.tiltakspenger.saksbehandling.felles.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.felles.nå
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak
import java.time.LocalDate
import java.time.LocalDateTime

interface UtbetalingsvedtakMother {

    fun utbetalingsvedtak(
        id: VedtakId = VedtakId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(LocalDate.now(), "1001"),
        fnr: Fnr = Fnr.random(),
        periode: Periode = Periode(2.januar(2023), 15.januar(2023)),
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
        meldekortBehandling: MeldekortBehandling.MeldekortBehandlet = ObjectMother.meldekortBehandlet(
            sakId = sakId,
            fnr = fnr,
            periode = periode,
            barnetilleggsPerioder = barnetilleggsPerioder,
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
            meldekortbehandling = meldekortBehandling,
            forrigeUtbetalingsvedtakId = forrigeUtbetalingsvedtakId,
            sendtTilUtbetaling = sendtTilUtbetaling,
            journalpostId = journalpostId,
            journalføringstidspunkt = journalføringstidspunkt,
            status = null,
        )
    }
}
