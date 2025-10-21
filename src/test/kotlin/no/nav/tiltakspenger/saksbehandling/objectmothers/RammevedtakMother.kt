package no.nav.tiltakspenger.saksbehandling.objectmothers

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtakstype
import no.nav.utsjekk.kontrakter.felles.Satstype
import java.time.LocalDate
import java.time.LocalDateTime

interface RammevedtakMother : MotherOfAllMothers {
    fun nyttRammevedtak(
        id: VedtakId = VedtakId.random(),
        opprettet: LocalDateTime = nå(clock),
        sakId: SakId = SakId.random(),
        periode: Periode = ObjectMother.virkningsperiode(),
        fnr: Fnr = Fnr.random(),
        behandling: Rammebehandling = ObjectMother.nyVedtattSøknadsbehandling(
            sakId = sakId,
            virkningsperiode = periode,
            fnr = fnr,
        ),
        vedtaksdato: LocalDate = 2.januar(2023),
        vedtaksType: Vedtakstype = Vedtakstype.INNVILGELSE,
        journalpostId: JournalpostId? = null,
        journalføringstidspunkt: LocalDateTime? = null,
        distribusjonId: DistribusjonId? = null,
        distribusjonstidspunkt: LocalDateTime? = null,
        sendtTilDatadeling: LocalDateTime? = null,
        brevJson: String? = null,
        forrigeUtbetalingId: UtbetalingId? = null,
    ) = Rammevedtak(
        id = id,
        opprettet = opprettet,
        sakId = sakId,
        behandling = behandling,
        vedtaksdato = vedtaksdato,
        vedtakstype = vedtaksType,
        periode = periode,
        journalpostId = journalpostId,
        journalføringstidspunkt = journalføringstidspunkt,
        distribusjonId = distribusjonId,
        distribusjonstidspunkt = distribusjonstidspunkt,
        sendtTilDatadeling = sendtTilDatadeling,
        brevJson = brevJson,
        utbetaling = behandling.tilRammevedtakUtbetaling(
            vedtakId = id,
            opprettet = opprettet,
            forrigeUtbetalingId = forrigeUtbetalingId,
        ),
    )

    fun nyRammevedtakInnvilgelse(
        id: VedtakId = VedtakId.random(),
        opprettet: LocalDateTime = nå(clock),
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        periode: Periode = ObjectMother.virkningsperiode(),
        antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
            periode,
        ),
        behandling: Rammebehandling = ObjectMother.nyVedtattSøknadsbehandling(
            sakId = sakId,
            virkningsperiode = periode,
            saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
            fnr = fnr,
            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
        ),
        vedtaksdato: LocalDate = 2.januar(2023),
        journalpostId: JournalpostId? = null,
        journalføringstidspunkt: LocalDateTime? = null,
        distribusjonId: DistribusjonId? = null,
        distribusjonstidspunkt: LocalDateTime? = null,
        sendtTilDatadeling: LocalDateTime? = null,
        brevJson: String? = null,
    ): Rammevedtak = nyttRammevedtak(
        id = id,
        opprettet = opprettet,
        sakId = sakId,
        behandling = behandling,
        vedtaksdato = vedtaksdato,
        vedtaksType = Vedtakstype.INNVILGELSE,
        periode = periode,
        journalpostId = journalpostId,
        journalføringstidspunkt = journalføringstidspunkt,
        distribusjonId = distribusjonId,
        distribusjonstidspunkt = distribusjonstidspunkt,
        sendtTilDatadeling = sendtTilDatadeling,
        brevJson = brevJson,
    )

    fun nyRammevedtakStans(
        id: VedtakId = VedtakId.random(),
        opprettet: LocalDateTime = nå(clock),
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        periode: Periode = ObjectMother.virkningsperiode(),
        behandling: Rammebehandling = ObjectMother.nyVedtattRevurderingStans(
            sakId = sakId,
            virkningsperiode = periode,
            saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
            fnr = fnr,
            førsteDagSomGirRett = periode.fraOgMed,
            sisteDagSomGirRett = periode.tilOgMed,
            stansFraOgMed = periode.fraOgMed,
            stansTilOgMed = periode.tilOgMed,
        ),
        vedtaksdato: LocalDate = 2.januar(2023),
        journalpostId: JournalpostId? = null,
        journalføringstidspunkt: LocalDateTime? = null,
        distribusjonId: DistribusjonId? = null,
        distribusjonstidspunkt: LocalDateTime? = null,
        sendtTilDatadeling: LocalDateTime? = null,
        brevJson: String? = null,
    ): Rammevedtak = nyttRammevedtak(
        id = id,
        opprettet = opprettet,
        sakId = sakId,
        behandling = behandling,
        vedtaksdato = vedtaksdato,
        vedtaksType = Vedtakstype.STANS,
        periode = periode,
        journalpostId = journalpostId,
        journalføringstidspunkt = journalføringstidspunkt,
        distribusjonId = distribusjonId,
        distribusjonstidspunkt = distribusjonstidspunkt,
        sendtTilDatadeling = sendtTilDatadeling,
        brevJson = brevJson,
    )

    fun Rammebehandling.tilRammevedtakUtbetaling(
        vedtakId: VedtakId,
        opprettet: LocalDateTime,
        forrigeUtbetalingId: UtbetalingId? = null,
    ): VedtattUtbetaling? {
        return this.utbetaling?.let {
            VedtattUtbetaling(
                id = UtbetalingId.random(),
                vedtakId = vedtakId,
                sakId = this.sakId,
                saksnummer = this.saksnummer,
                fnr = this.fnr,
                brukerNavkontor = it.navkontor,
                opprettet = opprettet,
                saksbehandler = this.saksbehandler!!,
                beslutter = this.beslutter!!,
                beregning = it.beregning,
                forrigeUtbetalingId = forrigeUtbetalingId,
                sendtTilUtbetaling = null,
                status = null,
                statusMetadata = Forsøkshistorikk.opprett(clock = clock),
                satstype = Satstype.DAGLIG,
            )
        }
    }
}
