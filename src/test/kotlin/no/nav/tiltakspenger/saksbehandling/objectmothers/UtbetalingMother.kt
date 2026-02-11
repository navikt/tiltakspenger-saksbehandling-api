package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.right
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.plus
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningId
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.maksAntallDeltattTiltaksdagerIMeldekortperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.navkontor
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingDetSkalHentesStatusFor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles.Satstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

interface UtbetalingMother : MotherOfAllMothers {

    fun utbetaling(
        beregningKilde: BeregningKilde = BeregningKilde.BeregningKildeMeldekort(
            id = MeldekortId.random(),
        ),
        id: UtbetalingId = UtbetalingId.random(),
        vedtakId: VedtakId = VedtakId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        brukerNavkontor: Navkontor = navkontor(),
        opprettet: LocalDateTime = nå(clock),
        saksbehandler: String = saksbehandler().navIdent,
        beslutter: String = beslutter().navIdent,
        beregning: Beregning = lagBeregning(
            beregningKilde = beregningKilde,
        ),
        forrigeUtbetalingId: UtbetalingId? = null,
        sendtTilUtbetaling: LocalDateTime? = null,
        status: Utbetalingsstatus? = null,
        statusMetadata: Forsøkshistorikk = Forsøkshistorikk.opprett(clock = clock),
        satstype: Satstype = Satstype.DAGLIG,
    ): VedtattUtbetaling {
        return VedtattUtbetaling(
            id = id,
            vedtakId = vedtakId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            brukerNavkontor = brukerNavkontor,
            opprettet = opprettet,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            beregning = beregning,
            forrigeUtbetalingId = forrigeUtbetalingId,
            statusMetadata = statusMetadata,
            satstype = satstype,
            sendtTilUtbetaling = sendtTilUtbetaling,
            status = status,
        )
    }

    fun lagBeregning(
        meldekortId: MeldekortId = MeldekortId.random(),
        beregningKilde: BeregningKilde = BeregningKilde.BeregningKildeMeldekort(meldekortId),
        startDato: LocalDate = LocalDate.of(2023, 1, 2),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(
            Periode(startDato, startDato.plusDays(13)),
        ),
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        maksDagerMedTiltakspengerForPeriode: Int = DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
        barnetilleggsPerioder: Periodisering<AntallBarn>? = null,
        beregningDager: NonEmptyList<MeldeperiodeBeregningDag> = maksAntallDeltattTiltaksdagerIMeldekortperiode(
            startDato,
            meldekortId,
            tiltakstype,
            barnetilleggsPerioder,
        ),
    ): Beregning {
        val beregninger = nonEmptyListOf(
            MeldeperiodeBeregning(
                id = BeregningId.random(),
                kjedeId = kjedeId,
                meldekortId = meldekortId,
                dager = beregningDager,
                beregningKilde = beregningKilde,
            ),
        )

        return when (beregningKilde) {
            is BeregningKilde.BeregningKildeBehandling -> Beregning(beregninger)
            is BeregningKilde.BeregningKildeMeldekort -> Beregning(beregninger)
        }
    }

    fun utbetalingDetSkalHentesStatusFor(
        utbetalingId: UtbetalingId = UtbetalingId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
        sakId: SakId = SakId.random(),
        opprettet: LocalDateTime = nå(clock),
        sendtTilUtbetalingstidspunkt: LocalDateTime = nå(fixedClock.plus(1, ChronoUnit.SECONDS)),
        forsøkshistorikk: Forsøkshistorikk = Forsøkshistorikk.opprett(clock = clock),
    ): UtbetalingDetSkalHentesStatusFor {
        return UtbetalingDetSkalHentesStatusFor(
            utbetalingId = utbetalingId,
            sakId = sakId,
            saksnummer = saksnummer,
            opprettet = opprettet,
            sendtTilUtbetalingstidspunkt = sendtTilUtbetalingstidspunkt,
            forsøkshistorikk = forsøkshistorikk,
        )
    }
}

// Mock validering av utbetaling. Kan fjernes når vi støtter feilutbetaling igjen.
suspend fun medTillattFeilutbetaling(block: suspend () -> Unit) {
    mockkStatic("no.nav.tiltakspenger.saksbehandling.utbetaling.domene.ValiderKanIverksetteUtbetalingKt")
    try {
        every { any<Simulering.Endring>().validerKanIverksetteUtbetaling() } returns Unit.right()
        block()
    } finally {
        unmockkStatic("no.nav.tiltakspenger.saksbehandling.utbetaling.domene.ValiderKanIverksetteUtbetalingKt")
    }
}
