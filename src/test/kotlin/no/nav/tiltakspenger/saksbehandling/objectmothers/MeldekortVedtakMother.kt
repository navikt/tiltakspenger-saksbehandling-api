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
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortVedtak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetaling
import java.time.LocalDate
import java.time.LocalDateTime

interface MeldekortVedtakMother : MotherOfAllMothers {

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
        forrigeUtbetalingVedtakId: VedtakId? = null,
        sendtTilUtbetaling: LocalDateTime? = null,
        journalpostId: JournalpostId? = null,
        journalføringstidspunkt: LocalDateTime? = null,
        opprettet: LocalDateTime = nå(clock),
        status: Utbetalingsstatus? = null,
        statusMetadata: Forsøkshistorikk = Forsøkshistorikk.opprett(clock = clock),
    ): MeldekortVedtak {
        return MeldekortVedtak(
            id = id,
            opprettet = opprettet,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            meldekortBehandling = meldekortBehandling,
            journalpostId = journalpostId,
            journalføringstidspunkt = journalføringstidspunkt,
            utbetaling = Utbetaling(
                beregning = meldekortBehandling.beregning,
                brukerNavkontor = meldekortBehandling.navkontor,
                vedtakId = id,
                forrigeUtbetalingVedtakId = forrigeUtbetalingVedtakId,
                sendtTilUtbetaling = sendtTilUtbetaling,
                status = null,
            ),
        )
    }

    fun utbetalingDetSkalHentesStatusFor(
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        vedtakId: VedtakId = VedtakId.random(),
        sakId: SakId = SakId.random(),
        opprettet: LocalDateTime = nå(clock),
        sendtTilUtbetalingstidspunkt: LocalDateTime = nå(fixedClock.plus(1, ChronoUnit.SECONDS)),
        forsøkshistorikk: Forsøkshistorikk? = Forsøkshistorikk.opprett(clock = clock),
    ): UtbetalingDetSkalHentesStatusFor {
        return UtbetalingDetSkalHentesStatusFor(
            sakId = sakId,
            saksnummer = saksnummer,
            vedtakId = vedtakId,
            opprettet = opprettet,
            sendtTilUtbetalingstidspunkt = sendtTilUtbetalingstidspunkt,
            forsøkshistorikk = forsøkshistorikk,
        )
    }
}
