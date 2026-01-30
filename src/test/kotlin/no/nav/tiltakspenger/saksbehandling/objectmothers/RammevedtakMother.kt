package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.nonEmptySetOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjortAvRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.utsjekk.kontrakter.felles.Satstype
import java.time.LocalDate
import java.time.LocalDateTime

interface RammevedtakMother : MotherOfAllMothers {
    fun nyttRammevedtak(
        id: VedtakId = VedtakId.random(),
        opprettet: LocalDateTime = nå(clock),
        sakId: SakId = SakId.random(),
        periode: Periode = ObjectMother.vedtaksperiode(),
        fnr: Fnr = Fnr.random(),
        omgjørRammevedtak: OmgjørRammevedtak = OmgjørRammevedtak.empty,
        innvilgelsesperioder: List<InnvilgelsesperiodeKommando> = listOf(
            innvilgelsesperiodeKommando(
                innvilgelsesperiode = periode,
            ),
        ),
        behandling: Rammebehandling = ObjectMother.nyVedtattSøknadsbehandling(
            sakId = sakId,
            saksopplysningsperiode = periode,
            innvilgelsesperioder = innvilgelsesperioder,
            saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
            fnr = fnr,
        ),
        vedtaksdato: LocalDate = 2.januar(2023),
        journalpostId: JournalpostId? = null,
        journalføringstidspunkt: LocalDateTime? = null,
        distribusjonId: DistribusjonId? = null,
        distribusjonstidspunkt: LocalDateTime? = null,
        sendtTilDatadeling: LocalDateTime? = null,
        brevJson: String? = null,
        forrigeUtbetalingId: UtbetalingId? = null,
        omgjortAvRammevedtakId: VedtakId? = null,
        utbetaling: VedtattUtbetaling? = behandling.tilRammevedtakUtbetaling(
            vedtakId = id,
            opprettet = opprettet,
            forrigeUtbetalingId = forrigeUtbetalingId,
        ),
    ) = Rammevedtak(
        id = id,
        opprettet = opprettet,
        sakId = sakId,
        behandling = behandling,
        vedtaksdato = vedtaksdato,
        periode = periode,
        journalpostId = journalpostId,
        journalføringstidspunkt = journalføringstidspunkt,
        distribusjonId = distribusjonId,
        distribusjonstidspunkt = distribusjonstidspunkt,
        sendtTilDatadeling = sendtTilDatadeling,
        brevJson = brevJson,
        utbetaling = utbetaling,
        omgjortAvRammevedtak = OmgjortAvRammevedtak.empty,
    )

    fun nyRammevedtakInnvilgelse(
        id: VedtakId = VedtakId.random(),
        opprettet: LocalDateTime = nå(clock),
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        innvilgelsesperioder: List<InnvilgelsesperiodeKommando> = listOf(
            innvilgelsesperiodeKommando(
                innvilgelsesperiode = ObjectMother.vedtaksperiode(),
            ),
        ),
        behandling: Rammebehandling = ObjectMother.nyVedtattSøknadsbehandling(
            sakId = sakId,
            saksopplysningsperiode = Periode(
                innvilgelsesperioder.first().periode.fraOgMed,
                innvilgelsesperioder.last().periode.tilOgMed,
            ),
            innvilgelsesperioder = innvilgelsesperioder,
            saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
            fnr = fnr,
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
        periode = behandling.innvilgelsesperioder!!.totalPeriode,
        journalpostId = journalpostId,
        journalføringstidspunkt = journalføringstidspunkt,
        distribusjonId = distribusjonId,
        distribusjonstidspunkt = distribusjonstidspunkt,
        sendtTilDatadeling = sendtTilDatadeling,
        brevJson = brevJson,
    )

    fun nyRammevedtakAvslag(
        id: VedtakId = VedtakId.random(),
        opprettet: LocalDateTime = nå(clock),
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        avslagsperiode: Periode = ObjectMother.vedtaksperiode(),
        behandling: Rammebehandling = ObjectMother.nyVedtattSøknadsbehandling(
            sakId = sakId,
            saksopplysningsperiode = avslagsperiode,
            saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
            fnr = fnr,
            resultat = SøknadsbehandlingType.AVSLAG,
            avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.Alder),
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
        periode = behandling.vedtaksperiode!!,
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
        periode: Periode = ObjectMother.vedtaksperiode(),
        omgjørRammevedtak: OmgjørRammevedtak = OmgjørRammevedtak.empty,
        behandling: Rammebehandling = ObjectMother.nyVedtattRevurderingStans(
            sakId = sakId,
            vedtaksperiode = periode,
            saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
            fnr = fnr,
            førsteDagSomGirRett = periode.fraOgMed,
            sisteDagSomGirRett = periode.tilOgMed,
            stansFraOgMed = periode.fraOgMed,
            omgjørRammevedtak = omgjørRammevedtak,
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
        periode = periode,
        journalpostId = journalpostId,
        journalføringstidspunkt = journalføringstidspunkt,
        distribusjonId = distribusjonId,
        distribusjonstidspunkt = distribusjonstidspunkt,
        sendtTilDatadeling = sendtTilDatadeling,
        brevJson = brevJson,
        omgjørRammevedtak = omgjørRammevedtak,
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

    fun nyRammevedtakOmgjøring(
        id: VedtakId = VedtakId.random(),
        opprettet: LocalDateTime = nå(clock),
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        søknadsbehandlingInnvilgelsesperiode: Periode = ObjectMother.revurderingVedtaksperiode(),
        omgjøringInnvilgelsesperiode: Periode = ObjectMother.revurderingVedtaksperiode(),
        behandling: Rammebehandling = ObjectMother.nyIverksattRevurderingOmgjøring(
            sakId = sakId,
            søknadsbehandlingInnvilgelsesperiode = søknadsbehandlingInnvilgelsesperiode,
            omgjøringInnvilgelsesperiode = omgjøringInnvilgelsesperiode,
            saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
            fnr = fnr,
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
        periode = søknadsbehandlingInnvilgelsesperiode,
        journalpostId = journalpostId,
        journalføringstidspunkt = journalføringstidspunkt,
        distribusjonId = distribusjonId,
        distribusjonstidspunkt = distribusjonstidspunkt,
        sendtTilDatadeling = sendtTilDatadeling,
        brevJson = brevJson,
    )
}
