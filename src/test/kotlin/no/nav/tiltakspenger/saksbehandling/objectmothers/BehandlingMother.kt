package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.NonEmptySet
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.NonBlankString.Companion.toNonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.periodisering.mars
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlinger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingsutfallGammel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendRevurderingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelHarIkkeRettighet
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.januarDateTime
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.AttesteringId
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
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

    fun nyOpprettetRevurdering(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        virkningsperiode: Periode = virkningsperiode(),
        hentSaksopplysninger: (Periode) -> Saksopplysninger = {
            saksopplysninger(
                fom = it.fraOgMed,
                tom = it.tilOgMed,
            )
        },
    ): Revurdering {
        return runBlocking {
            Revurdering.opprett(
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                saksbehandler = saksbehandler,
                saksopplysningsperiode = virkningsperiode,
                hentSaksopplysninger = hentSaksopplysninger,
                clock = clock,
            )
        }
    }

    fun nyRevurderingKlarTilBeslutning(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        sendtTilBeslutning: LocalDateTime? = null,
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev("nyRevurderingKlarTilBeslutning()"),
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("nyRevurderingKlarTilBeslutning()"),
        virkningsperiode: Periode = virkningsperiode(),
        saksopplysninger: Saksopplysninger = saksopplysninger(),
        valgteHjemler: List<ValgtHjemmelHarIkkeRettighet> = listOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
        stansDato: LocalDate,
        sisteDagSomGirRett: LocalDate,
    ): Revurdering {
        return this.nyOpprettetRevurdering(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            virkningsperiode = virkningsperiode,
            hentSaksopplysninger = { saksopplysninger },
        ).tilBeslutning(
            kommando = SendRevurderingTilBeslutningKommando(
                sakId = sakId,
                behandlingId = id,
                saksbehandler = saksbehandler,
                correlationId = CorrelationId.generate(),
                begrunnelse = begrunnelseVilkårsvurdering,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                valgteHjemler = valgteHjemler,
                stansDato = stansDato,
            ),
            sisteDagSomGirRett = sisteDagSomGirRett,
            clock = clock,
        )
    }

    fun nyVedtattRevurdering(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        beslutter: Saksbehandler = beslutter(),
        sendtTilBeslutning: LocalDateTime? = null,
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev("nyRevurderingKlarTilBeslutning()"),
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering = BegrunnelseVilkårsvurdering("nyRevurderingKlarTilBeslutning()"),
        virkningsperiode: Periode = virkningsperiode(),
        saksopplysninger: Saksopplysninger = saksopplysninger(),
        valgteHjemler: List<ValgtHjemmelHarIkkeRettighet> = listOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak),
        attestering: Attestering = godkjentAttestering(beslutter),
        stansDato: LocalDate,
        sisteDagSomGirRett: LocalDate,
    ): Revurdering {
        return nyRevurderingKlarTilBeslutning(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
            sendtTilBeslutning = sendtTilBeslutning,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            virkningsperiode = virkningsperiode,
            saksopplysninger = saksopplysninger,
            valgteHjemler = valgteHjemler,
            stansDato = stansDato,
            sisteDagSomGirRett = sisteDagSomGirRett,
        ).iverksett(
            utøvendeBeslutter = beslutter,
            attestering = attestering,
            clock = clock,
        ) as Revurdering
    }

    fun nyOpprettetSøknadsbehandling(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        søknad: Søknad = nySøknad(),
        hentSaksopplysninger: (Periode) -> Saksopplysninger = {
            saksopplysninger(
                fom = it.fraOgMed,
                tom = it.tilOgMed,
            )
        },
    ): Søknadsbehandling {
        return runBlocking {
            Søknadsbehandling.opprett(
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

    fun nySøknadsbehandlingKlarTilBeslutning(
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
        antallDagerPerMeldeperiode: Int = Behandling.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
        avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
        utfall: BehandlingsutfallGammel = BehandlingsutfallGammel.INNVILGELSE,
    ): Søknadsbehandling {
        return this.nyOpprettetSøknadsbehandling(
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
                behandlingsperiode = virkningsperiode,
                barnetillegg = barnetillegg,
                tiltaksdeltakelser = valgteTiltaksdeltakelser,
                antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
                avslagsgrunner = avslagsgrunner,
                utfall = SendSøknadsbehandlingTilBeslutningKommando.Utfall.INNVILGELSE,
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
        utfall: BehandlingsutfallGammel = BehandlingsutfallGammel.INNVILGELSE,
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
            virkningsperiode = virkningsperiode,
            saksopplysninger = saksopplysninger,
            valgteTiltaksdeltakelser = valgteTiltaksdeltakelser,
            oppgaveId = oppgaveId,
            utfall = utfall,
        ).taBehandling(beslutter)
    }

    fun nyBehandlingUnderkjent(
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
        utdøvendeBeslutter: Saksbehandler = beslutter(),
        utfall: BehandlingsutfallGammel = BehandlingsutfallGammel.INNVILGELSE,
        clock: Clock = fixedClock,
    ): Behandling {
        return nyBehandlingUnderBeslutning(
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
            virkningsperiode = virkningsperiode,
            saksopplysninger = saksopplysninger,
            valgteTiltaksdeltakelser = valgteTiltaksdeltakelser,
            oppgaveId = oppgaveId,
            utfall = utfall,
            clock = clock,
        ).sendTilbakeTilBehandling(
            utøvendeBeslutter = utdøvendeBeslutter,
            attestering = Attestering(
                id = AttesteringId.random(),
                status = Attesteringsstatus.SENDT_TILBAKE,
                begrunnelse = "nyBehandlingUnderkjent".toNonBlankString(),
                beslutter = "necessitatibus",
                tidspunkt = nå(clock),
            ),
        )
    }

    fun nyVedtattBehandling(
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
        utfall: BehandlingsutfallGammel = BehandlingsutfallGammel.INNVILGELSE,
        clock: Clock = fixedClock,
    ): Behandling {
        return nyBehandlingUnderBeslutning(
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
            virkningsperiode = virkningsperiode,
            saksopplysninger = saksopplysninger,
            valgteTiltaksdeltakelser = valgteTiltaksdeltakelser,
            oppgaveId = oppgaveId,
            utfall = utfall,
            clock = clock,
        ).iverksett(
            utøvendeBeslutter = beslutter,
            attestering = godkjentAttestering(beslutter),
            clock = clock,
        )
    }

    fun nyAvbruttBehandling(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        clock: Clock = fixedClock,
        avbruttAv: Saksbehandler = saksbehandler(),
        begrunnelse: String = "fordi",
        tidspunkt: LocalDateTime = LocalDateTime.now(clock),
    ): Søknadsbehandling {
        return nyOpprettetSøknadsbehandling(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            saksbehandler = saksbehandler,
        ).avbryt(
            avbruttAv = avbruttAv,
            begrunnelse = begrunnelse,
            tidspunkt = tidspunkt,
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
    søknad: Søknad = nySøknad(
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
    søknad: Søknad = nySøknad(
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
    antallDagerPerMeldeperiode: Int = Behandling.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
    avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
    utfall: SendSøknadsbehandlingTilBeslutningKommando.Utfall,
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
            behandlingsperiode = periode,
            barnetillegg = null,
            tiltaksdeltakelser = listOf(
                Pair(
                    periode,
                    sakMedFørstegangsbehandling.behandlinger.singleOrNullOrThrow()!!.saksopplysninger.tiltaksdeltagelse.first().eksternDeltagelseId,
                ),
            ),
            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
            avslagsgrunner = avslagsgrunner,
            utfall = utfall,
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
    utfall: SendSøknadsbehandlingTilBeslutningKommando.Utfall,
    antallDagerPerMeldeperiode: Int = Behandling.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
): Sak {
    val vilkårsvurdert = førstegangsbehandlingTilBeslutter(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        utfall = utfall,
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
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

suspend fun TestApplicationContext.søknadssbehandlingIverksatt(
    periode: Periode = ObjectMother.virkningsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
    correlationId: CorrelationId = CorrelationId.generate(),
    utfall: SendSøknadsbehandlingTilBeslutningKommando.Utfall,
    antallDagerPerMeldeperiode: Int = Behandling.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
): Sak {
    val tac = this
    val underBeslutning = førstegangsbehandlingUnderBeslutning(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        utfall = utfall,
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
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

suspend fun TestApplicationContext.søknadsbehandlingIverksattMedMeldeperioder(
    periode: Periode = ObjectMother.virkningsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
    clock: Clock = fixedClock,
    correlationId: CorrelationId = CorrelationId.generate(),
    utfall: SendSøknadsbehandlingTilBeslutningKommando.Utfall = SendSøknadsbehandlingTilBeslutningKommando.Utfall.INNVILGELSE,
    antallDagerPerMeldeperiode: Int = Behandling.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
): Sak {
    val (sak, meldeperioder) = søknadssbehandlingIverksatt(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        correlationId = correlationId,
        utfall = utfall,
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
    ).genererMeldeperioder(clock)

    this.meldekortContext.meldeperiodeRepo.lagre(meldeperioder)

    return sak
}

suspend fun TestApplicationContext.meldekortBehandlingOpprettet(
    periode: Periode = ObjectMother.virkningsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
    correlationId: CorrelationId = CorrelationId.generate(),
): Sak {
    val tac = this
    val sak = søknadssbehandlingIverksatt(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        utfall = SendSøknadsbehandlingTilBeslutningKommando.Utfall.INNVILGELSE,
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
    tac.meldekortContext.taMeldekortBehandlingService.taMeldekortBehandling(
        meldekortId = sak.meldekortBehandlinger.first().id,
        saksbehandler = beslutter,
        correlationId = correlationId,
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
