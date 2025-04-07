package no.nav.tiltakspenger.saksbehandling.objectmothers

import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.førsteNovember24
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.periodisering.mars
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.januarDateTime
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.AttesteringId
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Systembruker
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.IverksettMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nySøknad
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.personSøknad
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksopplysninger
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.person.PersonopplysningerSøker
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

interface BehandlingMother : MotherOfAllMothers {
    /** Felles default vurderingsperiode for testdatatypene */
    fun virkningsperiode() = Periode(1.januar(2023), 31.mars(2023))
    fun revurderingsperiode() = Periode(2.januar(2023), 31.mars(2023))

    fun godkjentAttestering(beslutter: Saksbehandler = beslutter()): Attestering =
        Attestering(
            id = AttesteringId.random(),
            status = Attesteringsstatus.GODKJENT,
            begrunnelse = null,
            beslutter = beslutter.navIdent,
            tidspunkt = nå(clock),
        )

    // TODO - nyBehandling() starter fra underBehandling - vi vil helst begynne fra denne, og bruke funksjoner for å bevege oss videre
    fun nyOpprettetFørstegangsbehandling(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        søknad: Søknad = ObjectMother.nySøknad(),
        hentSaksopplysninger: (Periode) -> Saksopplysninger = {
            ObjectMother.saksopplysninger(
                fom = it.fraOgMed,
                tom = it.tilOgMed,
            )
        },
    ): Behandling {
        return runBlocking {
            Behandling.opprettSøknadsbehandling(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                søknad = søknad,
                saksbehandler = saksbehandler,
                hentSaksopplysninger = hentSaksopplysninger,
                clock = clock,
            ).getOrFail()
        }
    }

    // TODO - ikke bruk denne. Bruk [nyOpprettetFørstegangsbehandling]
    fun nyBehandling(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        virkningsperiode: Periode? = virkningsperiode(),
        behandlingstype: Behandlingstype = Behandlingstype.FØRSTEGANGSBEHANDLING,
        søknad: Søknad? = if (behandlingstype == Behandlingstype.FØRSTEGANGSBEHANDLING) ObjectMother.nySøknad() else null,
        saksbehandlerIdent: String = saksbehandler().navIdent,
        sendtTilBeslutning: LocalDateTime? = null,
        beslutterIdent: String? = null,
        saksopplysninger: Saksopplysninger = saksopplysninger(),
        status: Behandlingsstatus = Behandlingsstatus.UNDER_BEHANDLING,
        attesteringer: List<Attestering> = emptyList(),
        opprettet: LocalDateTime = førsteNovember24,
        iverksattTidspunkt: LocalDateTime? = null,
        sendtTilDatadeling: LocalDateTime? = null,
        sistEndret: LocalDateTime = førsteNovember24,
        oppgaveId: OppgaveId = ObjectMother.oppgaveId(),
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev? = null,
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering? = null,
        saksopplysningsperiode: Periode = virkningsperiode(),
        barnetillegg: Barnetillegg? = null,
        valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser? = null,
        avbrutt: Avbrutt? = null,
    ): Behandling {
        return Behandling(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            virkningsperiode = virkningsperiode,
            søknad = søknad,
            saksbehandler = saksbehandlerIdent,
            sendtTilBeslutning = sendtTilBeslutning,
            beslutter = beslutterIdent,
            saksopplysninger = saksopplysninger,
            status = status,
            attesteringer = attesteringer,
            opprettet = opprettet,
            iverksattTidspunkt = iverksattTidspunkt,
            sendtTilDatadeling = sendtTilDatadeling,
            sistEndret = sistEndret,
            behandlingstype = behandlingstype,
            oppgaveId = oppgaveId,
            valgtHjemmelHarIkkeRettighet = emptyList(),
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            saksopplysningsperiode = saksopplysningsperiode,
            barnetillegg = barnetillegg,
            valgteTiltaksdeltakelser = valgteTiltaksdeltakelser,
            avbrutt = avbrutt,
        )
    }

    fun nyFørstegangsbehandlingKlarTilBeslutning(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        søknad: Søknad = nySøknad(),
        sendtTilBeslutning: LocalDateTime? = null,
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev("nyFørstegangsbehandlingKlarTilBeslutning()"),
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("nyFørstegangsbehandlingKlarTilBeslutning()"),
        barnetillegg: Barnetillegg? = null,
        virkningsperiode: Periode = virkningsperiode(),
        saksopplysninger: Saksopplysninger = saksopplysninger(),
        valgteTiltaksdeltakelser: List<Pair<Periode, String>> = saksopplysninger.tiltaksdeltagelse.map {
            Pair(virkningsperiode, it.eksternDeltagelseId)
        },
        oppgaveId: OppgaveId = ObjectMother.oppgaveId(),
    ): Behandling {
        return this.nyOpprettetFørstegangsbehandling(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            søknad = søknad,
            hentSaksopplysninger = { saksopplysninger },
        ).tilBeslutning(
            kommando = SendSøknadsbehandlingTilBeslutningKommando(
                sakId = sakId,
                behandlingId = id,
                saksbehandler = saksbehandler,
                correlationId = CorrelationId.generate(),
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                innvilgelsesperiode = virkningsperiode,
                barnetillegg = barnetillegg,
                tiltaksdeltakelser = valgteTiltaksdeltakelser,
            ),
            clock = fixedClock,
        )
    }

    fun nyBehandlingUnderBeslutning(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        sendtTilBeslutning: LocalDateTime? = null,
        søknad: Søknad = nySøknad(),
        beslutter: Saksbehandler = beslutter(),
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev("nyBehandlingUnderBeslutning()"),
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("nyBehandlingUnderBeslutning()"),
        barnetillegg: Barnetillegg? = null,
        virkningsperiode: Periode = virkningsperiode(),
        saksopplysninger: Saksopplysninger = saksopplysninger(),
        valgteTiltaksdeltakelser: List<Pair<Periode, String>> = saksopplysninger.tiltaksdeltagelse.map {
            Pair(virkningsperiode, it.eksternDeltagelseId)
        },
        oppgaveId: OppgaveId = ObjectMother.oppgaveId(),
        clock: Clock = fixedClock,
    ): Behandling {
        return nyFørstegangsbehandlingKlarTilBeslutning(
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
            virkningsperiode = virkningsperiode,
            saksopplysninger = saksopplysninger,
            valgteTiltaksdeltakelser = valgteTiltaksdeltakelser,
            oppgaveId = oppgaveId,
        ).taBehandling(beslutter)
    }

    fun nyVedtattBehandling(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        virkningsperiode: Periode = virkningsperiode(),
        behandlingstype: Behandlingstype = Behandlingstype.FØRSTEGANGSBEHANDLING,
        søknad: Søknad? = if (behandlingstype == Behandlingstype.FØRSTEGANGSBEHANDLING) ObjectMother.nySøknad() else null,
        saksbehandlerIdent: String = saksbehandler().navIdent,
        sendtTilBeslutning: LocalDateTime = førsteNovember24,
        beslutterIdent: String = beslutter().navIdent,
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = virkningsperiode.fraOgMed,
            tom = virkningsperiode.tilOgMed,
        ),
        attesteringer: List<Attestering> = emptyList(),
        opprettet: LocalDateTime = førsteNovember24,
        iverksattTidspunkt: LocalDateTime = førsteNovember24,
        sendtTilDatadeling: LocalDateTime? = null,
        sistEndret: LocalDateTime = førsteNovember24,
        oppgaveId: OppgaveId = ObjectMother.oppgaveId(),
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev("nyVedtattBehandling()"),
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering? = null,
        saksopplysningsperiode: Periode = virkningsperiode,
        barnetillegg: Barnetillegg? = null,
        valgteTiltaksdeltakelser: ValgteTiltaksdeltakelser? = ValgteTiltaksdeltakelser(
            Periodisering(
                PeriodeMedVerdi(
                    saksopplysninger.tiltaksdeltagelse.first(),
                    virkningsperiode,
                ),
            ),
        ),
    ): Behandling {
        return nyBehandling(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            virkningsperiode = virkningsperiode,
            søknad = søknad,
            saksbehandlerIdent = saksbehandlerIdent,
            sendtTilBeslutning = sendtTilBeslutning,
            beslutterIdent = beslutterIdent,
            saksopplysninger = saksopplysninger,
            status = Behandlingsstatus.VEDTATT,
            attesteringer = attesteringer,
            opprettet = opprettet,
            iverksattTidspunkt = iverksattTidspunkt,
            sendtTilDatadeling = sendtTilDatadeling,
            sistEndret = sistEndret,
            behandlingstype = behandlingstype,
            oppgaveId = oppgaveId,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            saksopplysningsperiode = saksopplysningsperiode,
            barnetillegg = barnetillegg,
            valgteTiltaksdeltakelser = valgteTiltaksdeltakelser,
            avbrutt = null,
        )
    }
}

suspend fun TestApplicationContext.nySøknad(
    periode: Periode = ObjectMother.virkningsperiode(),
    fnr: Fnr = Fnr.random(),
    fornavn: String = "Fornavn",
    etternavn: String = "Etternavn",
    personopplysningerFraSøknad: Søknad.Personopplysninger = personSøknad(
        fnr = fnr,
        fornavn = fornavn,
        etternavn = etternavn,
    ),
    personopplysningerForBrukerFraPdl: PersonopplysningerSøker = ObjectMother.personopplysningKjedeligFyr(
        fnr = fnr,
    ),
    deltarPåIntroduksjonsprogram: Boolean = false,
    deltarPåKvp: Boolean = false,
    tidsstempelHosOss: LocalDateTime = 1.januarDateTime(2022),
    tiltaksdeltagelse: Tiltaksdeltagelse? = null,
    søknadstiltak: Søknadstiltak? = tiltaksdeltagelse?.toSøknadstiltak(),
    sak: Sak = ObjectMother.nySak(fnr = fnr),
    søknad: Søknad = ObjectMother.nySøknad(
        fnr = fnr,
        personopplysninger = personopplysningerFraSøknad,
        tidsstempelHosOss = tidsstempelHosOss,
        søknadstiltak = søknadstiltak ?: ObjectMother.søknadstiltak(
            deltakelseFom = periode.fraOgMed,
            deltakelseTom = periode.tilOgMed,
        ),
        intro = if (deltarPåIntroduksjonsprogram) Søknad.PeriodeSpm.Ja(periode) else Søknad.PeriodeSpm.Nei,
        kvp = if (deltarPåKvp) Søknad.PeriodeSpm.Ja(periode) else Søknad.PeriodeSpm.Nei,
        sakId = sak.id,
        saksnummer = sak.saksnummer,
    ),
    systembruker: Systembruker = ObjectMother.systembrukerLageHendelser(),
): Søknad {
    this.søknadContext.søknadService.nySøknad(søknad, systembruker)
    this.leggTilPerson(fnr, personopplysningerForBrukerFraPdl, tiltaksdeltagelse ?: søknad.tiltak.toTiltak())
    return søknad
}

/**
 * Oppretter sak, sender inn søknad og starter behandling.
 * @param søknad Dersom du sender inn denne, bør du og sende inn tiltak+fnr for at de skal henge sammen.
 */
suspend fun TestApplicationContext.startSøknadsbehandling(
    periode: Periode = ObjectMother.virkningsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    fødselsdato: LocalDate = 1.januar(2000),
    personopplysningerForBrukerFraPdl: PersonopplysningerSøker = ObjectMother.personopplysningKjedeligFyr(
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
    tiltaksdeltagelse: Tiltaksdeltagelse? = null,
    søknadstiltak: Søknadstiltak? = tiltaksdeltagelse?.toSøknadstiltak(),
    sak: Sak = ObjectMother.nySak(fnr = fnr),
    søknad: Søknad = ObjectMother.nySøknad(
        fnr = fnr,
        personopplysninger = personopplysningerFraSøknad,
        tidsstempelHosOss = tidsstempelHosOss,
        søknadstiltak = søknadstiltak ?: ObjectMother.søknadstiltak(
            deltakelseFom = periode.fraOgMed,
            deltakelseTom = periode.tilOgMed,
        ),
        intro = if (deltarPåIntroduksjonsprogram) Søknad.PeriodeSpm.Ja(periode) else Søknad.PeriodeSpm.Nei,
        kvp = if (deltarPåKvp) Søknad.PeriodeSpm.Ja(periode) else Søknad.PeriodeSpm.Nei,
        sakId = sak.id,
        saksnummer = sak.saksnummer,
    ),
    correlationId: CorrelationId = CorrelationId.generate(),
): Sak {
    this.sakContext.sakRepo.opprettSak(sak)
    this.nySøknad(
        periode = periode,
        fnr = fnr,
        fornavn = fornavn,
        etternavn = etternavn,
        søknad = søknad,
        personopplysningerFraSøknad = personopplysningerFraSøknad,
        personopplysningerForBrukerFraPdl = personopplysningerForBrukerFraPdl,
        tiltaksdeltagelse = tiltaksdeltagelse,
        sak = sak,
    )
    return sak.copy(
        behandlinger = Behandlinger(
            sak.behandlinger.behandlinger + this.behandlingContext.startSøknadsbehandlingService.startSøknadsbehandling(
                søknad.id,
                sak.id,
                saksbehandler,
                correlationId = correlationId,
            ).getOrFail(),
        ),
    )
}

suspend fun TestApplicationContext.førstegangsbehandlingTilBeslutter(
    periode: Periode = ObjectMother.virkningsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    correlationId: CorrelationId = CorrelationId.generate(),
    fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev("Fritekst"),
    begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("Begrunnelse"),
): Sak {
    val sakMedFørstegangsbehandling = startSøknadsbehandling(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
    )

    this.behandlingContext.sendBehandlingTilBeslutningService.sendFørstegangsbehandlingTilBeslutning(
        SendSøknadsbehandlingTilBeslutningKommando(
            sakId = sakMedFørstegangsbehandling.id,
            behandlingId = sakMedFørstegangsbehandling.behandlinger.singleOrNullOrThrow()!!.id,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            innvilgelsesperiode = periode,
            barnetillegg = null,
            tiltaksdeltakelser = listOf(
                Pair(
                    periode,
                    sakMedFørstegangsbehandling.behandlinger.singleOrNullOrThrow()!!.saksopplysninger.tiltaksdeltagelse.first().eksternDeltagelseId,
                ),
            ),
        ),
    ).getOrFail()
    return this.sakContext.sakService.hentForSakId(
        sakMedFørstegangsbehandling.id,
        saksbehandler,
        correlationId = correlationId,
    ).getOrFail()
}

suspend fun TestApplicationContext.førstegangsbehandlingUnderBeslutning(
    periode: Periode = ObjectMother.virkningsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
    correlationId: CorrelationId = CorrelationId.generate(),
): Sak {
    val vilkårsvurdert = førstegangsbehandlingTilBeslutter(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
    )
    this.behandlingContext.taBehandlingService.taBehandling(
        vilkårsvurdert.behandlinger.singleOrNullOrThrow()!!.id,
        beslutter,
        correlationId = correlationId,
    )
    return this.sakContext.sakService.hentForSakId(
        vilkårsvurdert.id,
        saksbehandler,
        correlationId = correlationId,
    ).getOrFail()
}

suspend fun TestApplicationContext.førstegangsbehandlingIverksatt(
    periode: Periode = ObjectMother.virkningsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
    correlationId: CorrelationId = CorrelationId.generate(),
): Sak {
    val tac = this
    val underBeslutning = førstegangsbehandlingUnderBeslutning(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )
    runBlocking {
        tac.behandlingContext.iverksettBehandlingService.iverksett(
            behandlingId = underBeslutning.behandlinger.singleOrNullOrThrow()!!.id,
            beslutter = beslutter,
            correlationId = correlationId,
            sakId = underBeslutning.id,
        )
    }
    return this.sakContext.sakService.hentForSakId(
        underBeslutning.id,
        saksbehandler,
        correlationId = correlationId,
    ).getOrFail()
}

suspend fun TestApplicationContext.meldekortBehandlingOpprettet(
    periode: Periode = ObjectMother.virkningsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
    correlationId: CorrelationId = CorrelationId.generate(),
): Sak {
    val tac = this
    val sak = førstegangsbehandlingIverksatt(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )
    tac.meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
        sakId = sak.id,
        kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
        saksbehandler = saksbehandler,
        correlationId = correlationId,
    )
    return this.sakContext.sakService.hentForSakId(
        sak.id,
        saksbehandler,
        correlationId = correlationId,
    ).getOrFail()
}

suspend fun TestApplicationContext.meldekortTilBeslutter(
    periode: Periode = ObjectMother.virkningsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
    correlationId: CorrelationId = CorrelationId.generate(),
): Sak {
    val tac = this
    val sak = meldekortBehandlingOpprettet(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )
    tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
        sak.meldekortBehandlinger.first().tilSendMeldekortTilBeslutterKommando(
            saksbehandler,
        ),
    )
    return this.sakContext.sakService.hentForSakId(sak.id, saksbehandler, correlationId = correlationId)
        .getOrFail()
}

/**
 * Genererer også utbetalingsvedtak, men sender ikke til utbetaling.
 */
suspend fun TestApplicationContext.førsteMeldekortIverksatt(
    periode: Periode = ObjectMother.virkningsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
    correlationId: CorrelationId = CorrelationId.generate(),
): Sak {
    val tac = this
    val sak = meldekortTilBeslutter(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )
    tac.meldekortContext.iverksettMeldekortService.iverksettMeldekort(
        IverksettMeldekortKommando(
            meldekortId = sak.meldekortBehandlinger.first().id,
            sakId = sak.id,
            beslutter = beslutter,
            correlationId = correlationId,
        ),
    )
    // Emulerer at jobben kjører
    tac.utbetalingContext.sendUtbetalingerService.send()
    return this.sakContext.sakService.hentForSakId(sak.id, saksbehandler, correlationId = correlationId).getOrFail()
}

suspend fun TestApplicationContext.andreMeldekortIverksatt(
    periode: Periode = ObjectMother.virkningsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
    correlationId: CorrelationId = CorrelationId.generate(),
): Sak {
    val tac = this
    val sak = førsteMeldekortIverksatt(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )

    tac.meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
        sakId = sak.id,
        kjedeId = sak.meldeperiodeKjeder[1].kjedeId,
        saksbehandler = saksbehandler,
        correlationId = correlationId,
    )

    return this.sakContext.sakService.hentForSakId(sak.id, saksbehandler, correlationId = correlationId).getOrFail()
}
