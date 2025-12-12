package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.utbetaling
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import java.time.LocalDate
import java.time.LocalDateTime

interface MeldekortvedtakMother : MotherOfAllMothers {

    fun meldekortvedtak(
        id: VedtakId = VedtakId.random(),
        utbetalingId: UtbetalingId = UtbetalingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(LocalDate.now(), "1001"),
        fnr: Fnr = Fnr.random(),
        periode: Periode = Periode(2.januar(2023), 15.januar(2023)),
        barnetilleggsPerioder: Periodisering<AntallBarn>? = null,
        meldekortBehandling: MeldekortBehandletManuelt = ObjectMother.meldekortBehandletManuelt(
            sakId = sakId,
            fnr = fnr,
            saksnummer = saksnummer,
            periode = periode,
            barnetilleggsPerioder = barnetilleggsPerioder,
        ),
        forrigeUtbetalingId: UtbetalingId? = null,
        sendtTilUtbetaling: LocalDateTime? = null,
        journalpostId: JournalpostId? = null,
        journalføringstidspunkt: LocalDateTime? = null,
        opprettet: LocalDateTime = nå(clock),
        status: Utbetalingsstatus? = null,
        statusMetadata: Forsøkshistorikk = Forsøkshistorikk.opprett(clock = clock),
    ): Meldekortvedtak {
        return Meldekortvedtak(
            id = id,
            opprettet = opprettet,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            meldekortBehandling = meldekortBehandling,
            journalpostId = journalpostId,
            journalføringstidspunkt = journalføringstidspunkt,
            utbetaling = utbetaling(
                id = utbetalingId,
                sendtTilUtbetaling = sendtTilUtbetaling,
                status = status,
                forrigeUtbetalingId = forrigeUtbetalingId,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                beregning = meldekortBehandling.beregning,
                brukerNavkontor = meldekortBehandling.navkontor,
                vedtakId = id,
                opprettet = opprettet,
                saksbehandler = meldekortBehandling.saksbehandler,
                beslutter = meldekortBehandling.beslutter!!,
                statusMetadata = statusMetadata,
            ),
        )
    }
}
