package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsvedtak
import java.time.LocalDate
import java.time.LocalDateTime

interface UtbetalingsvedtakMother : MotherOfAllMothers {

    fun utbetalingsvedtak(
        id: VedtakId = VedtakId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(LocalDate.now(), "1001"),
        fnr: Fnr = Fnr.random(),
        periode: Periode = Periode(2.januar(2023), 15.januar(2023)),
        barnetilleggsPerioder: SammenhengendePeriodisering<AntallBarn>? = null,
        meldekortBehandling: MeldekortBehandletManuelt = ObjectMother.meldekortBehandletManuelt(
            sakId = sakId,
            fnr = fnr,
            saksnummer = saksnummer,
            periode = periode,
            barnetilleggsPerioder = barnetilleggsPerioder,
        ),
        forrigeUtbetalingsvedtakId: VedtakId? = null,
        sendtTilUtbetaling: LocalDateTime? = null,
        journalpostId: JournalpostId? = null,
        journalføringstidspunkt: LocalDateTime? = null,
        opprettet: LocalDateTime = nå(clock),
    ): Utbetalingsvedtak {
        return Utbetalingsvedtak(
            id = id,
            opprettet = opprettet,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            forrigeUtbetalingsvedtakId = forrigeUtbetalingsvedtakId,
            sendtTilUtbetaling = sendtTilUtbetaling,
            journalpostId = journalpostId,
            journalføringstidspunkt = journalføringstidspunkt,
            status = null,
            beregning = meldekortBehandling.beregning,
            saksbehandler = meldekortBehandling.saksbehandler,
            beslutter = meldekortBehandling.beslutter!!,
            brukerNavkontor = meldekortBehandling.navkontor,
            rammevedtak = meldekortBehandling.rammevedtak!!,
            automatiskBehandlet = false,
        )
    }
}
