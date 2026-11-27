package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.NonEmptySet
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.avbryt.avbryt
import no.nav.tiltakspenger.saksbehandling.behandling.domene.iverksett.iverksett
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.oppdater
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.HentSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ta.taBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.tilBeslutter.SendBehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.underkjenn.underkjenn
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.AttesteringId
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyInnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.oppdaterSøknadsbehandlingAvslagKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.oppdaterSøknadsbehandlingInnvilgelseKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksopplysninger
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import java.time.Clock
import java.time.LocalDateTime
import no.nav.tiltakspenger.saksbehandling.behandling.domene.tilBeslutter.tilBeslutning as sendRammebehandlingTilBeslutning

interface BehandlingMother : MotherOfAllMothers {
    /** Felles default vedtaksperiode for testdatatypene */
    fun vedtaksperiode() = 1.januar(2023) til 31.mars(2023)

    fun godkjentAttestering(beslutter: Saksbehandler = beslutter(), clock: Clock = this.clock): Attestering =
        Attestering(
            id = AttesteringId.random(),
            status = Attesteringsstatus.GODKJENT,
            begrunnelse = null,
            beslutter = beslutter.navIdent,
            tidspunkt = nå(clock),
        )

    fun underkjentAttestering(
        id: AttesteringId = AttesteringId.random(),
        begrunnelse: NonBlankString = "Manglende dokumentasjon".toNonBlankString(),
        beslutter: Saksbehandler = beslutter(),
        clock: Clock = this.clock,
    ): Attestering =
        Attestering(
            id = AttesteringId.random(),
            status = Attesteringsstatus.SENDT_TILBAKE,
            begrunnelse = begrunnelse,
            beslutter = beslutter.navIdent,
            tidspunkt = nå(clock),
        )

    fun nyOpprettetSøknadsbehandling(
        id: RammebehandlingId = RammebehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = ObjectMother.nesteSaksnummer(),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(),
        hentSaksopplysninger: HentSaksopplysninger = { _, _, _, _, _, _ ->
            saksopplysninger(
                fom = søknad.tiltak.deltakelseFom,
                tom = søknad.tiltak.deltakelseTom,
            )
        },
        clock: Clock = this.clock,
        sak: Sak = ObjectMother.nySak(sakId = sakId, saksnummer = saksnummer, fnr = fnr, søknader = listOf(søknad)),
        correlationId: CorrelationId = CorrelationId.generate(),
        klagebehandling: Klagebehandling? = null,
    ): Søknadsbehandling {
        return runBlocking {
            Søknadsbehandling.opprett(
                sak = sak,
                søknad = søknad,
                saksbehandler = saksbehandler,
                hentSaksopplysninger = hentSaksopplysninger,
                clock = clock,
                correlationId = correlationId,
                klagebehandling = klagebehandling,
            ).second.copy(id = id)
        }
    }

    fun nyOpprettetAutomatiskSøknadsbehandling(
        id: RammebehandlingId = RammebehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = ObjectMother.nesteSaksnummer(),
        fnr: Fnr = Fnr.random(),
        søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(),
        hentSaksopplysninger: HentSaksopplysninger = { _, _, _, _, _, _ ->
            saksopplysninger(
                fom = søknad.tiltak.deltakelseFom,
                tom = søknad.tiltak.deltakelseTom,
            )
        },
        sak: Sak = ObjectMother.nySak(sakId = sakId, saksnummer = saksnummer, fnr = fnr, søknader = listOf(søknad)),
        correlationId: CorrelationId = CorrelationId.generate(),
    ): Søknadsbehandling {
        return runBlocking {
            Søknadsbehandling.opprettAutomatiskBehandling(
                søknad = søknad,
                hentSaksopplysninger = hentSaksopplysninger,
                clock = clock,
                sak = sak,
                correlationId = correlationId,
            ).first
        }
    }

    fun nyAutomatiskSøknadsbehandlingManuellBehandling(
        id: RammebehandlingId = RammebehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = ObjectMother.nesteSaksnummer(),
        fnr: Fnr = Fnr.random(),
        søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(),
        hentSaksopplysninger: HentSaksopplysninger = { _, _, _, _, _, _ ->
            saksopplysninger(
                fom = søknad.tiltak.deltakelseFom,
                tom = søknad.tiltak.deltakelseTom,
            )
        },
        manueltBehandlesGrunner: List<ManueltBehandlesGrunn> = listOf(ManueltBehandlesGrunn.SOKNAD_HAR_KVP),
        sak: Sak = ObjectMother.nySak(sakId = sakId, saksnummer = saksnummer, fnr = fnr, søknader = listOf(søknad)),
        correlationId: CorrelationId = CorrelationId.generate(),
    ): Søknadsbehandling {
        return runBlocking {
            val behandling = Søknadsbehandling.opprettAutomatiskBehandling(
                søknad = søknad,
                hentSaksopplysninger = hentSaksopplysninger,
                clock = clock,
                sak = sak,
                correlationId = correlationId,
            ).first
            return@runBlocking behandling.tilManuellBehandling(
                manueltBehandlesGrunner = manueltBehandlesGrunner,
                clock = clock,
            )
        }
    }

    fun oppdatertSøknadsbehandling(
        id: RammebehandlingId = RammebehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = ObjectMother.nesteSaksnummer(),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(),
        sendtTilBeslutning: LocalDateTime? = null,
        fritekstTilVedtaksbrev: String = "nySøknadsbehandlingKlarTilBeslutning()",
        begrunnelseVilkårsvurdering: String = "nySøknadsbehandlingKlarTilBeslutning()",
        vedtaksperiode: Periode = vedtaksperiode(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = vedtaksperiode.fraOgMed,
            tom = vedtaksperiode.tilOgMed,
        ),
        oppgaveId: OppgaveId = ObjectMother.oppgaveId(),
        innvilgelsesperioder: List<InnvilgelsesperiodeKommando> = listOf(
            innvilgelsesperiodeKommando(
                innvilgelsesperiode = vedtaksperiode,
                tiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser.first(),
            ),
        ),
        avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
        resultat: SøknadsbehandlingsresultatType = SøknadsbehandlingsresultatType.INNVILGELSE,
        clock: Clock = this.clock,
        omgjørRammevedtak: OmgjørRammevedtak = OmgjørRammevedtak.empty,
    ): Søknadsbehandling {
        return this.nyOpprettetSøknadsbehandling(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            søknad = søknad,
            hentSaksopplysninger = { _, _, _, _, _, _ -> saksopplysninger },
            clock = clock,
        ).oppdater(
            when (resultat) {
                SøknadsbehandlingsresultatType.INNVILGELSE -> oppdaterSøknadsbehandlingInnvilgelseKommando(
                    sakId = sakId,
                    saksbehandler = saksbehandler,
                    behandlingId = id,
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                    begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                    innvilgelsesperioder = innvilgelsesperioder.medDeltakelseFra(saksopplysninger),
                    barnetillegg = barnetillegg,
                )

                SøknadsbehandlingsresultatType.AVSLAG -> oppdaterSøknadsbehandlingAvslagKommando(
                    sakId = sakId,
                    saksbehandler = saksbehandler,
                    behandlingId = id,
                    correlationId = CorrelationId.generate(),
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                    begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                    avslagsgrunner = avslagsgrunner!!,
                )

                SøknadsbehandlingsresultatType.IKKE_VALGT -> throw IllegalArgumentException("Støtter ikke resultat $resultat")
            },
            clock = clock,
            utbetaling = null,
            omgjørRammevedtak = omgjørRammevedtak,
        ).getOrFail()
    }

    fun nySøknadsbehandlingKlarTilBeslutning(
        id: RammebehandlingId = RammebehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = ObjectMother.nesteSaksnummer(),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(),
        sendtTilBeslutning: LocalDateTime? = null,
        fritekstTilVedtaksbrev: String = "nySøknadsbehandlingKlarTilBeslutning()",
        begrunnelseVilkårsvurdering: String = "nySøknadsbehandlingKlarTilBeslutning()",
        vedtaksperiode: Periode = vedtaksperiode(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = vedtaksperiode.fraOgMed,
            tom = vedtaksperiode.tilOgMed,
        ),
        oppgaveId: OppgaveId = ObjectMother.oppgaveId(),
        innvilgelsesperioder: List<InnvilgelsesperiodeKommando> = listOf(
            innvilgelsesperiodeKommando(
                innvilgelsesperiode = vedtaksperiode,
                tiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser.first(),
            ),
        ),
        avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
        resultat: SøknadsbehandlingsresultatType = SøknadsbehandlingsresultatType.INNVILGELSE,
        clock: Clock = this.clock,
        omgjørRammevedtak: OmgjørRammevedtak = OmgjørRammevedtak.empty,
    ): Søknadsbehandling {
        return this.oppdatertSøknadsbehandling(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            søknad = søknad,
            sendtTilBeslutning = sendtTilBeslutning,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            barnetillegg = barnetillegg,
            vedtaksperiode = vedtaksperiode,
            saksopplysninger = saksopplysninger,
            oppgaveId = oppgaveId,
            avslagsgrunner = avslagsgrunner,
            innvilgelsesperioder = innvilgelsesperioder,
            resultat = resultat,
            omgjørRammevedtak = omgjørRammevedtak,
            clock = clock,
        ).tilBeslutning(
            saksbehandler = saksbehandler,
            clock = clock,
        ) as Søknadsbehandling
    }

    fun nySøknadsbehandlingUnderBeslutning(
        id: RammebehandlingId = RammebehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = ObjectMother.nesteSaksnummer(),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        sendtTilBeslutning: LocalDateTime? = null,
        søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(),
        beslutter: Saksbehandler = beslutter(),
        fritekstTilVedtaksbrev: String = "nyBehandlingUnderBeslutning()",
        begrunnelseVilkårsvurdering: String = "nyBehandlingUnderBeslutning()",
        vedtaksperiode: Periode = vedtaksperiode(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = vedtaksperiode.fraOgMed,
            tom = vedtaksperiode.tilOgMed,
        ),
        oppgaveId: OppgaveId = ObjectMother.oppgaveId(),
        resultat: SøknadsbehandlingsresultatType = SøknadsbehandlingsresultatType.INNVILGELSE,
        innvilgelsesperioder: List<InnvilgelsesperiodeKommando> = listOf(
            innvilgelsesperiodeKommando(
                innvilgelsesperiode = vedtaksperiode,
                tiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser.first(),
            ),
        ),
        avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
        omgjørRammevedtak: OmgjørRammevedtak = OmgjørRammevedtak.empty,
        clock: Clock = fixedClock,
    ): Søknadsbehandling {
        return nySøknadsbehandlingKlarTilBeslutning(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            søknad = søknad,
            sendtTilBeslutning = sendtTilBeslutning,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            barnetillegg = barnetillegg,
            vedtaksperiode = vedtaksperiode,
            saksopplysninger = saksopplysninger,
            oppgaveId = oppgaveId,
            resultat = resultat,
            innvilgelsesperioder = innvilgelsesperioder,
            avslagsgrunner = avslagsgrunner,
            omgjørRammevedtak = omgjørRammevedtak,
            clock = clock,
        ).taBehandling(beslutter, clock).getOrFail().first as Søknadsbehandling
    }

    @Suppress("unused")
    fun nySøknadsbehandlingUnderkjent(
        id: RammebehandlingId = RammebehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = ObjectMother.nesteSaksnummer(),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        sendtTilBeslutning: LocalDateTime? = null,
        søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(),
        beslutter: Saksbehandler = beslutter(),
        fritekstTilVedtaksbrev: String = "nyBehandlingUnderBeslutning()",
        begrunnelseVilkårsvurdering: String = "nyBehandlingUnderBeslutning()",
        vedtaksperiode: Periode = vedtaksperiode(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = vedtaksperiode.fraOgMed,
            tom = vedtaksperiode.tilOgMed,
        ),
        innvilgelsesperioder: List<InnvilgelsesperiodeKommando> = listOf(
            innvilgelsesperiodeKommando(
                innvilgelsesperiode = vedtaksperiode,
                tiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser.first(),
            ),
        ),
        oppgaveId: OppgaveId = ObjectMother.oppgaveId(),
        utdøvendeBeslutter: Saksbehandler = beslutter(),
        resultat: SøknadsbehandlingsresultatType = SøknadsbehandlingsresultatType.INNVILGELSE,
        omgjørRammevedtak: OmgjørRammevedtak = OmgjørRammevedtak.empty,
        clock: Clock = fixedClock,
    ): Søknadsbehandling {
        return nySøknadsbehandlingUnderBeslutning(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            sendtTilBeslutning = sendtTilBeslutning,
            søknad = søknad,
            beslutter = beslutter,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            barnetillegg = barnetillegg,
            vedtaksperiode = vedtaksperiode,
            innvilgelsesperioder = innvilgelsesperioder,
            saksopplysninger = saksopplysninger,
            oppgaveId = oppgaveId,
            resultat = resultat,
            omgjørRammevedtak = omgjørRammevedtak,
            clock = clock,
        ).underkjenn(
            utøvendeBeslutter = utdøvendeBeslutter,
            attestering = Attestering(
                id = AttesteringId.random(),
                status = Attesteringsstatus.SENDT_TILBAKE,
                begrunnelse = "nyBehandlingUnderkjent".toNonBlankString(),
                beslutter = "necessitatibus",
                tidspunkt = nå(clock),
            ),
            clock = clock,
        ).getOrFail().first as Søknadsbehandling
    }

    /**
     * @param innvilgelsesperioder vil default utledes fra [saksopplysningsperiode].
     * Hvis du overstyrer denne, bør du også overstyre [saksopplysningsperiode].
     */
    fun nyVedtattSøknadsbehandling(
        id: RammebehandlingId = RammebehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = ObjectMother.nesteSaksnummer(),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        sendtTilBeslutning: LocalDateTime? = null,
        søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(),
        beslutter: Saksbehandler = beslutter(),
        fritekstTilVedtaksbrev: String = "nyBehandlingUnderBeslutning()",
        begrunnelseVilkårsvurdering: String = "nyBehandlingUnderBeslutning()",
        saksopplysningsperiode: Periode = vedtaksperiode(),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = saksopplysningsperiode.fraOgMed,
            tom = saksopplysningsperiode.tilOgMed,
        ),
        oppgaveId: OppgaveId = ObjectMother.oppgaveId(),
        resultat: SøknadsbehandlingsresultatType = SøknadsbehandlingsresultatType.INNVILGELSE,
        innvilgelsesperioder: List<InnvilgelsesperiodeKommando> = listOf(
            innvilgelsesperiodeKommando(
                innvilgelsesperiode = saksopplysningsperiode,
                tiltaksdeltakelse = saksopplysninger.tiltaksdeltakelser.first(),
            ),
        ),
        barnetillegg: Barnetillegg = barnetillegg(
            antallBarn = AntallBarn.ZERO,
            periode = Periode(
                innvilgelsesperioder.first().periode.fraOgMed,
                innvilgelsesperioder.last().periode.tilOgMed,
            ),
        ),
        clock: Clock = fixedClock,
        avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
        omgjørRammevedtak: OmgjørRammevedtak = OmgjørRammevedtak.empty,
        correlationId: CorrelationId = CorrelationId.generate(),
    ): Søknadsbehandling {
        return nySøknadsbehandlingUnderBeslutning(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            sendtTilBeslutning = sendtTilBeslutning,
            søknad = søknad,
            beslutter = beslutter,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            barnetillegg = barnetillegg,
            vedtaksperiode = saksopplysningsperiode,
            saksopplysninger = saksopplysninger,
            oppgaveId = oppgaveId,
            resultat = resultat,
            clock = clock,
            innvilgelsesperioder = innvilgelsesperioder,
            avslagsgrunner = avslagsgrunner,
            omgjørRammevedtak = omgjørRammevedtak,
        ).iverksett(
            utøvendeBeslutter = beslutter,
            attestering = godkjentAttestering(beslutter, clock),
            correlationId = correlationId,
            clock = clock,
        ).getOrFail().first as Søknadsbehandling
    }

    fun nyAvbruttSøknadsbehandling(
        id: RammebehandlingId = RammebehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = ObjectMother.nesteSaksnummer(),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        clock: Clock = fixedClock,
        avbruttAv: Saksbehandler = saksbehandler(),
        begrunnelse: String = "fordi",
        tidspunkt: LocalDateTime = nå(clock),
        hentSaksopplysninger: (Periode) -> Saksopplysninger = {
            saksopplysninger(
                fom = it.fraOgMed,
                tom = it.tilOgMed,
            )
        },
    ): Søknadsbehandling {
        return nyOpprettetSøknadsbehandling(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            hentSaksopplysninger = { _, _, _, _, _, _ -> saksopplysninger() },
            clock = clock,
        ).avbryt(
            avbruttAv = avbruttAv,
            begrunnelse = begrunnelse.toNonBlankString(),
            tidspunkt = tidspunkt,
            skalAvbryteSøknad = true,
        ).getOrFail() as Søknadsbehandling
    }
}

fun Rammebehandling.tilBeslutning(
    saksbehandler: Saksbehandler = saksbehandler(),
    correlationId: CorrelationId = CorrelationId.generate(),
    clock: Clock = fixedClock,
): Rammebehandling {
    return this.sendRammebehandlingTilBeslutning(
        kommando = SendBehandlingTilBeslutningKommando(
            sakId = this.sakId,
            behandlingId = this.id,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
        ),
        clock = clock,
    ).getOrFail()
}

/**
 * Binder innvilgelseskommandoene til saksopplysningenes faktiske tiltaksdeltakelse.
 * Kallstedene uttrykker hvilke perioder som innvilges; deltakelsen eies av saksopplysningene og har unik id per test.
 * Tiltaksdeltakelser som allerede peker på en deltakelse i saksopplysningene røres ikke.
 */
fun List<InnvilgelsesperiodeKommando>.medDeltakelseFra(saksopplysninger: Saksopplysninger): List<InnvilgelsesperiodeKommando> {
    return map { kommando ->
        kommando.copy(
            internDeltakelseIder = kommando.internDeltakelseIder.map { internDeltakelseId ->
                if (saksopplysninger.tiltaksdeltakelser.any { it.internDeltakelseId == internDeltakelseId }) {
                    internDeltakelseId
                } else {
                    val dekkende = saksopplysninger.tiltaksdeltakelser.firstOrNull { deltakelse ->
                        deltakelse.lukketPeriode?.inneholderHele(kommando.periode) == true
                    }
                    val erstatning = dekkende ?: saksopplysninger.tiltaksdeltakelser.firstOrNull()
                    erstatning?.internDeltakelseId ?: internDeltakelseId
                }
            },
        )
    }
}
