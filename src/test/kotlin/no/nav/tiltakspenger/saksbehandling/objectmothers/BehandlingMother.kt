package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.NonEmptySet
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendBehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.HentSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.januarDateTime
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.AttesteringId
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.IverksettMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyInnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.oppdaterSøknadsbehandlingAvslagKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.oppdaterSøknadsbehandlingInnvilgelseKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.personSøknad
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksopplysninger
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(),
        hentSaksopplysninger: HentSaksopplysninger = { _, _, _, _, _ ->
            saksopplysninger(
                fom = søknad.tiltak.deltakelseFom,
                tom = søknad.tiltak.deltakelseTom,
            )
        },
        clock: Clock = this.clock,
        sak: Sak = ObjectMother.nySak(sakId = sakId, saksnummer = saksnummer, fnr = fnr, søknader = listOf(søknad)),
        correlationId: CorrelationId = CorrelationId.generate(),
        automatiskBehandling: Boolean = false,
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
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(),
        hentSaksopplysninger: HentSaksopplysninger = { _, _, _, _, _ ->
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
            )
        }
    }

    fun nyAutomatiskSøknadsbehandlingManuellBehandling(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(),
        hentSaksopplysninger: HentSaksopplysninger = { _, _, _, _, _ ->
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
            )
            return@runBlocking behandling.tilManuellBehandling(
                manueltBehandlesGrunner = manueltBehandlesGrunner,
                clock = clock,
            )
        }
    }

    fun oppdatertSøknadsbehandling(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(),
        sendtTilBeslutning: LocalDateTime? = null,
        fritekstTilVedtaksbrev: String = "nySøknadsbehandlingKlarTilBeslutning()",
        begrunnelseVilkårsvurdering: String = "nySøknadsbehandlingKlarTilBeslutning()",
        vedtaksperiode: Periode = vedtaksperiode(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(vedtaksperiode),
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
        automatiskBehandling: Boolean = false,
    ): Søknadsbehandling {
        return this.nyOpprettetSøknadsbehandling(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            søknad = søknad,
            hentSaksopplysninger = { _, _, _, _, _ -> saksopplysninger },
            clock = clock,
        ).oppdater(
            when (resultat) {
                SøknadsbehandlingsresultatType.INNVILGELSE -> oppdaterSøknadsbehandlingInnvilgelseKommando(
                    sakId = sakId,
                    saksbehandler = saksbehandler,
                    behandlingId = id,
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                    begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                    innvilgelsesperioder = innvilgelsesperioder,
                    barnetillegg = barnetillegg,
                    automatiskSaksbehandlet = automatiskBehandling,
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
            },
            clock = clock,
            utbetaling = null,
            omgjørRammevedtak = omgjørRammevedtak,
        ).getOrFail()
    }

    fun nySøknadsbehandlingKlarTilBeslutning(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(),
        sendtTilBeslutning: LocalDateTime? = null,
        fritekstTilVedtaksbrev: String = "nySøknadsbehandlingKlarTilBeslutning()",
        begrunnelseVilkårsvurdering: String = "nySøknadsbehandlingKlarTilBeslutning()",
        vedtaksperiode: Periode = vedtaksperiode(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(vedtaksperiode),
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
        automatiskBehandling: Boolean = false,
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
            automatiskBehandling = automatiskBehandling,
        ).tilBeslutning(
            saksbehandler = saksbehandler,
            clock = clock,
        ) as Søknadsbehandling
    }

    fun nySøknadsbehandlingUnderBeslutning(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        sendtTilBeslutning: LocalDateTime? = null,
        søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(),
        beslutter: Saksbehandler = beslutter(),
        fritekstTilVedtaksbrev: String = "nyBehandlingUnderBeslutning()",
        begrunnelseVilkårsvurdering: String = "nyBehandlingUnderBeslutning()",
        vedtaksperiode: Periode = vedtaksperiode(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(vedtaksperiode),
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
        automatiskBehandling: Boolean = false,
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
            automatiskBehandling = automatiskBehandling,
        ).taBehandling(beslutter, clock) as Søknadsbehandling
    }

    @Suppress("unused")
    fun nySøknadsbehandlingUnderkjent(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        sendtTilBeslutning: LocalDateTime? = null,
        søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(),
        beslutter: Saksbehandler = beslutter(),
        fritekstTilVedtaksbrev: String = "nyBehandlingUnderBeslutning()",
        begrunnelseVilkårsvurdering: String = "nyBehandlingUnderBeslutning()",
        vedtaksperiode: Periode = vedtaksperiode(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(vedtaksperiode),
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
        ) as Søknadsbehandling
    }

    /**
     * @param innvilgelsesperioder vil default utledes fra [saksopplysningsperiode]. Hvis du overstyrer denne, bør du også overstyre [saksopplysningsperiode].
     */
    fun nyVedtattSøknadsbehandling(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
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
        ) as Søknadsbehandling
    }

    fun nyAvbruttSøknadsbehandling(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        clock: Clock = fixedClock,
        avbruttAv: Saksbehandler = saksbehandler(),
        begrunnelse: String = "fordi",
        tidspunkt: LocalDateTime = LocalDateTime.now(clock),
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
            hentSaksopplysninger = { _, _, _, _, _ -> saksopplysninger() },
            clock = clock,
        ).avbryt(
            avbruttAv = avbruttAv,
            begrunnelse = begrunnelse.toNonBlankString(),
            tidspunkt = tidspunkt,
            skalAvbryteSøknad = true,
        )
    }
}

fun TestApplicationContext.nyInnvilgbarSøknad(
    periode: Periode = ObjectMother.vedtaksperiode(),
    fnr: Fnr = Fnr.random(),
    fornavn: String = "Fornavn",
    etternavn: String = "Etternavn",
    personopplysningerFraSøknad: Søknad.Personopplysninger = personSøknad(
        fnr = fnr,
        fornavn = fornavn,
        etternavn = etternavn,
    ),
    personopplysningerForBrukerFraPdl: EnkelPerson = ObjectMother.personopplysningKjedeligFyr(
        fnr = fnr,
    ),
    deltarPåIntroduksjonsprogram: Boolean = false,
    deltarPåKvp: Boolean = false,
    tidsstempelHosOss: LocalDateTime = 1.januarDateTime(2022),
    tiltaksdeltakelse: Tiltaksdeltakelse? = null,
    søknadstiltak: Søknadstiltak? = tiltaksdeltakelse?.toSøknadstiltak(),
    sak: Sak = ObjectMother.nySak(fnr = fnr),
    søknadId: SøknadId = SøknadId.random(),
    søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(
        fnr = fnr,
        personopplysninger = personopplysningerFraSøknad,
        tidsstempelHosOss = tidsstempelHosOss,
        søknadstiltak = søknadstiltak ?: ObjectMother.søknadstiltak(
            deltakelseFom = periode.fraOgMed,
            deltakelseTom = periode.tilOgMed,
        ),
        intro = if (deltarPåIntroduksjonsprogram) {
            Søknad.PeriodeSpm.Ja(
                fraOgMed = periode.fraOgMed,
                tilOgMed = periode.tilOgMed,
            )
        } else {
            Søknad.PeriodeSpm.Nei
        },
        kvp = if (deltarPåKvp) {
            Søknad.PeriodeSpm.Ja(
                fraOgMed = periode.fraOgMed,
                tilOgMed = periode.tilOgMed,
            )
        } else {
            Søknad.PeriodeSpm.Nei
        },
        sakId = sak.id,
        saksnummer = sak.saksnummer,
        id = søknadId,
    ),
): Søknad {
    this.søknadContext.søknadService.nySøknad(søknad)
    this.leggTilPerson(fnr, personopplysningerForBrukerFraPdl, tiltaksdeltakelse ?: søknad.tiltak.toTiltak())
    return søknad
}

/**
 * Oppretter sak, sender inn søknad og starter behandling.
 * @param søknad Dersom du sender inn denne, bør du og sende inn tiltak+fnr for at de skal henge sammen.
 */
suspend fun TestApplicationContext.startSøknadsbehandling(
    periode: Periode = ObjectMother.vedtaksperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    fødselsdato: LocalDate = 1.januar(2000),
    personopplysningerForBrukerFraPdl: EnkelPerson = ObjectMother.personopplysningKjedeligFyr(
        fnr = fnr,
        fødselsdato = fødselsdato,
    ),
    deltarPåIntroduksjonsprogram: Boolean = false,
    deltarPåKvp: Boolean = false,
    fornavn: String = "Fornavn",
    etternavn: String = "Etternavn",
    personopplysningerFraSøknad: Søknad.Personopplysninger = personSøknad(
        fnr = fnr,
        fornavn = fornavn,
        etternavn = etternavn,
    ),
    tidsstempelHosOss: LocalDateTime = 1.januarDateTime(2022),
    tiltaksdeltakelseId: String = UUID.randomUUID().toString(),
    internTiltaksdeltakelseId: TiltaksdeltakerId = TiltaksdeltakerId.random(),
    sak: Sak = ObjectMother.nySak(fnr = fnr),
    søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(
        fnr = fnr,
        personopplysninger = personopplysningerFraSøknad,
        tidsstempelHosOss = tidsstempelHosOss,
        søknadstiltak = ObjectMother.søknadstiltak(
            id = tiltaksdeltakelseId,
            deltakelseFom = periode.fraOgMed,
            deltakelseTom = periode.tilOgMed,
            tiltaksdeltakerId = internTiltaksdeltakelseId,
        ),
        intro = if (deltarPåIntroduksjonsprogram) {
            Søknad.PeriodeSpm.Ja(
                fraOgMed = periode.fraOgMed,
                tilOgMed = periode.tilOgMed,
            )
        } else {
            Søknad.PeriodeSpm.Nei
        },
        kvp = if (deltarPåKvp) {
            Søknad.PeriodeSpm.Ja(
                fraOgMed = periode.fraOgMed,
                tilOgMed = periode.tilOgMed,
            )
        } else {
            Søknad.PeriodeSpm.Nei
        },
        sakId = sak.id,
        saksnummer = sak.saksnummer,
    ),
    correlationId: CorrelationId = CorrelationId.generate(),
): Pair<Sak, Søknadsbehandling> {
    this.sakContext.sakRepo.opprettSak(sak)
    this.nyInnvilgbarSøknad(
        periode = periode,
        fnr = fnr,
        fornavn = fornavn,
        etternavn = etternavn,
        søknad = søknad,
        personopplysningerFraSøknad = personopplysningerFraSøknad,
        personopplysningerForBrukerFraPdl = personopplysningerForBrukerFraPdl,
        tiltaksdeltakelse = søknad.tiltak.toTiltak(),
        sak = sak,
    )
    val behandling = this.behandlingContext.startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(
        søknad,
        correlationId = correlationId,
    )
    val behandlingUnderBehandling = behandling.copy(
        status = Rammebehandlingsstatus.UNDER_BEHANDLING,
        saksbehandler = saksbehandler.navIdent,
    ).also {
        this.behandlingContext.rammebehandlingRepo.lagre(it)
    }
    return sak.leggTilSøknadsbehandling(behandlingUnderBehandling) to behandlingUnderBehandling
}

suspend fun TestApplicationContext.søknadsbehandlingTilBeslutter(
    periode: Periode = ObjectMother.vedtaksperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    correlationId: CorrelationId = CorrelationId.generate(),
    fritekstTilVedtaksbrev: String = "Fritekst",
    begrunnelseVilkårsvurdering: String = "Begrunnelse",
    avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
    innvilgelsesperioder: List<InnvilgelsesperiodeKommando> = listOf(innvilgelsesperiodeKommando(innvilgelsesperiode = periode)),
    barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(periode),
    resultat: SøknadsbehandlingsresultatType,
    automatiskBehandling: Boolean = false,
): Sak {
    val (sakMedSøknadsbehandling) = startSøknadsbehandling(
        periode = periode,
        fnr = fnr,
        internTiltaksdeltakelseId = innvilgelsesperioder.first().internDeltakelseId,
    )
    val behandling = sakMedSøknadsbehandling.rammebehandlinger.singleOrNullOrThrow()!! as Søknadsbehandling

    this.behandlingContext.oppdaterRammebehandlingService.oppdater(
        when (resultat) {
            SøknadsbehandlingsresultatType.INNVILGELSE -> oppdaterSøknadsbehandlingInnvilgelseKommando(
                sakId = sakMedSøknadsbehandling.id,
                behandlingId = behandling.id,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                innvilgelsesperioder = innvilgelsesperioder,
                barnetillegg = barnetillegg,
                automatiskSaksbehandlet = automatiskBehandling,
            )

            SøknadsbehandlingsresultatType.AVSLAG -> oppdaterSøknadsbehandlingAvslagKommando(
                sakId = sakMedSøknadsbehandling.id,
                behandlingId = behandling.id,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                avslagsgrunner = avslagsgrunner!!,
            )
        },
    ).getOrFail()

    this.behandlingContext.sendRammebehandlingTilBeslutningService.sendTilBeslutning(
        kommando = SendBehandlingTilBeslutningKommando(
            sakId = sakMedSøknadsbehandling.id,
            behandlingId = behandling.id,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
        ),
    )

    return this.sakContext.sakService.hentForSakId(
        sakMedSøknadsbehandling.id,
    )
}

suspend fun TestApplicationContext.søknadsbehandlingUnderBeslutning(
    periode: Periode = ObjectMother.vedtaksperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
    resultat: SøknadsbehandlingsresultatType,
    barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(periode),
    avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
    innvilgelsesperioder: List<InnvilgelsesperiodeKommando> = listOf(
        innvilgelsesperiodeKommando(
            innvilgelsesperiode = periode,
        ),
    ),
): Sak {
    val vilkårsvurdert = søknadsbehandlingTilBeslutter(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        resultat = resultat,
        innvilgelsesperioder = innvilgelsesperioder,
        barnetillegg = barnetillegg,
        avslagsgrunner = avslagsgrunner,
    )
    this.behandlingContext.taRammebehandlingService.taBehandling(
        vilkårsvurdert.id,
        vilkårsvurdert.rammebehandlinger.singleOrNullOrThrow()!!.id,
        beslutter,
    )
    return this.sakContext.sakService.hentForSakId(
        vilkårsvurdert.id,
    )
}

suspend fun TestApplicationContext.søknadssbehandlingIverksatt(
    periode: Periode = ObjectMother.vedtaksperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
    resultat: SøknadsbehandlingsresultatType,
    innvilgelsesperioder: List<InnvilgelsesperiodeKommando> = listOf(
        innvilgelsesperiodeKommando(
            innvilgelsesperiode = periode,
        ),
    ),
    barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(periode),
    avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
    correlationId: CorrelationId = CorrelationId.generate(),
): Pair<Sak, Søknadsbehandling> {
    val tac = this
    val underBeslutning = søknadsbehandlingUnderBeslutning(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        resultat = resultat,
        innvilgelsesperioder = innvilgelsesperioder,
        barnetillegg = barnetillegg,
        avslagsgrunner = avslagsgrunner,
    )
    val behandling = tac.behandlingContext.iverksettRammebehandlingService.iverksettRammebehandling(
        rammebehandlingId = underBeslutning.rammebehandlinger.singleOrNullOrThrow()!!.id,
        beslutter = beslutter,
        correlationId = correlationId,
        sakId = underBeslutning.id,
    ).getOrFail().second

    return this.sakContext.sakService.hentForSakId(behandling.sakId) to behandling as Søknadsbehandling
}

suspend fun TestApplicationContext.søknadsbehandlingIverksattMedMeldeperioder(
    periode: Periode = ObjectMother.vedtaksperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
    clock: Clock = fixedClock,
    resultat: SøknadsbehandlingsresultatType = SøknadsbehandlingsresultatType.INNVILGELSE,
    innvilgelsesperioder: List<InnvilgelsesperiodeKommando> = listOf(
        innvilgelsesperiodeKommando(
            innvilgelsesperiode = periode,
        ),
    ),
    barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(periode),
): Sak {
    val (sak, meldeperioder) = søknadssbehandlingIverksatt(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        resultat = resultat,
        innvilgelsesperioder = innvilgelsesperioder,
        barnetillegg = barnetillegg,
    ).first.genererMeldeperioder(clock)

    this.meldekortContext.meldeperiodeRepo.lagre(meldeperioder)

    return sak
}

/**
 * Oppretter sak, søknad, iverksetter søknadsbehandling og oppretter meldekortbehandling
 */
suspend fun TestApplicationContext.meldekortBehandlingOpprettet(
    innvilgelsesperiode: Periode = ObjectMother.vedtaksperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
): Sak {
    val tac = this
    val (sak) = søknadssbehandlingIverksatt(
        periode = innvilgelsesperiode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        resultat = SøknadsbehandlingsresultatType.INNVILGELSE,
    )
    tac.meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
        sakId = sak.id,
        kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
        saksbehandler = saksbehandler,
    )
    return this.sakContext.sakService.hentForSakId(
        sak.id,
    )
}

/**
 * Oppretter sak, søknad, iverksetter søknadsbehandling og oppretter meldekortbehandling
 */
suspend fun TestApplicationContext.meldekortBehandlingOppdatert(
    innvilgelsesperiode: Periode = ObjectMother.vedtaksperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
): Sak {
    val tac = this
    val sak = meldekortBehandlingOpprettet(
        innvilgelsesperiode = innvilgelsesperiode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )
    tac.meldekortContext.oppdaterMeldekortService.oppdaterMeldekort(
        sak.meldekortbehandlinger.first().tilOppdaterMeldekortKommando(
            saksbehandler,
        ),
    )
    return this.sakContext.sakService.hentForSakId(
        sak.id,
    )
}

/**
 * Oppretter sak, søknad, iverksetter søknadsbehandling, oppretter meldekortbehandling og sender den til beslutter
 */
suspend fun TestApplicationContext.meldekortTilBeslutter(
    innvilgelsesperiode: Periode = ObjectMother.vedtaksperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
): Sak {
    val tac = this
    val sak = meldekortBehandlingOppdatert(
        innvilgelsesperiode = innvilgelsesperiode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )
    tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
        sak.meldekortbehandlinger.first().tilSendMeldekortTilBeslutterKommando(
            saksbehandler,
        ),
    ).getOrFail()
    return this.sakContext.sakService.hentForSakId(sak.id)
}

/**
 * Oppretter sak, søknad, iverksetter søknadsbehandling, oppretter og iverksetter første meldekortbehandling.
 *
 * Genererer og sender også utbetaling for meldekortet
 */
suspend fun TestApplicationContext.førsteMeldekortIverksatt(
    innvilgelsesperiode: Periode = ObjectMother.vedtaksperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
    correlationId: CorrelationId = CorrelationId.generate(),
): Sak {
    val tac = this
    val sak = meldekortTilBeslutter(
        innvilgelsesperiode = innvilgelsesperiode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )
    val meldekortId = sak.meldekortbehandlinger.first().id
    tac.meldekortContext.taMeldekortBehandlingService.taMeldekortBehandling(
        sakId = sak.id,
        meldekortId = meldekortId,
        saksbehandler = beslutter,
    )
    tac.meldekortContext.iverksettMeldekortService.iverksettMeldekort(
        IverksettMeldekortKommando(
            meldekortId = meldekortId,
            sakId = sak.id,
            beslutter = beslutter,
            correlationId = correlationId,
        ),
    )
    // Emulerer at jobben kjører
    tac.utbetalingContext.sendUtbetalingerService.sendUtbetalingerTilHelved()
    return this.sakContext.sakService.hentForSakId(sak.id)
}

suspend fun TestApplicationContext.andreMeldekortOpprettet(
    periode: Periode = ObjectMother.vedtaksperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
): Sak {
    val tac = this
    val sak = førsteMeldekortIverksatt(
        innvilgelsesperiode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )

    val (_, meldekortbehandling) = tac.meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
        sakId = sak.id,
        kjedeId = sak.meldeperiodeKjeder[1].kjedeId,
        saksbehandler = saksbehandler,
    ).getOrFail()

    tac.meldekortContext.oppdaterMeldekortService.oppdaterMeldekort(
        kommando = meldekortbehandling.tilOppdaterMeldekortKommando(saksbehandler),
    )

    return this.sakContext.sakService.hentForSakId(sak.id)
}

fun Rammebehandling.tilBeslutning(
    saksbehandler: Saksbehandler = saksbehandler(),
    correlationId: CorrelationId = CorrelationId.generate(),
    clock: Clock = fixedClock,
): Rammebehandling {
    return this.tilBeslutning(
        kommando = SendBehandlingTilBeslutningKommando(
            sakId = this.sakId,
            behandlingId = this.id,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
        ),
        clock = clock,
    ).getOrFail()
}
