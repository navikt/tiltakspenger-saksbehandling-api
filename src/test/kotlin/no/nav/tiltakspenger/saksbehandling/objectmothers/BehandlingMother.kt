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
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterSøknadsbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendBehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.HentSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.januarDateTime
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.AttesteringId
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.felles.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.infra.route.AntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.tilAntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.IverksettMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.nyInnvilgbarSøknad
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
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.ValgteTiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.TiltaksdeltakelsePeriodeDTO
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

interface BehandlingMother : MotherOfAllMothers {
    /** Felles default vurderingsperiode for testdatatypene */
    fun virkningsperiode() = 1.januar(2023) til 31.mars(2023)

    fun Rammebehandling.tiltaksdeltakelseDTO(): List<TiltaksdeltakelsePeriodeDTO> {
        val tiltaksdeltakelse = this.saksopplysninger.tiltaksdeltakelser.single()

        return listOf(
            TiltaksdeltakelsePeriodeDTO(
                eksternDeltagelseId = tiltaksdeltakelse.eksternDeltakelseId,
                periode = tiltaksdeltakelse.periode!!.toDTO(),
            ),
        )
    }

    fun ValgteTiltaksdeltakelser.tiltaksdeltakelseDTO(): List<TiltaksdeltakelsePeriodeDTO> {
        return this.verdier.map { tiltaksdeltakelse ->
            TiltaksdeltakelsePeriodeDTO(
                eksternDeltagelseId = tiltaksdeltakelse.eksternDeltakelseId,
                periode = tiltaksdeltakelse.periode!!.toDTO(),
            )
        }
    }

    fun Rammebehandling.antallDagerPerMeldeperiodeDTO(
        periode: Periode,
        antallDager: Int = DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
    ): List<AntallDagerPerMeldeperiodeDTO> {
        return SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(antallDager),
            periode,
        ).tilAntallDagerPerMeldeperiodeDTO()
    }

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
    ): Søknadsbehandling {
        return runBlocking {
            Søknadsbehandling.opprett(
                sak = sak,
                søknad = søknad,
                saksbehandler = saksbehandler,
                hentSaksopplysninger = hentSaksopplysninger,
                clock = clock,
                correlationId = correlationId,
            ).copy(id = id)
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
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev.createOrThrow("nySøknadsbehandlingKlarTilBeslutning()"),
        begrunnelseVilkårsvurdering: Begrunnelse = Begrunnelse.createOrThrow("nySøknadsbehandlingKlarTilBeslutning()"),
        virkningsperiode: Periode = virkningsperiode(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(virkningsperiode),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = virkningsperiode.fraOgMed,
            tom = virkningsperiode.tilOgMed,
        ),
        valgteTiltaksdeltakelser: List<Pair<Periode, String>> = saksopplysninger.tiltaksdeltakelser.map {
            Pair(virkningsperiode, it.eksternDeltakelseId)
        },
        oppgaveId: OppgaveId = ObjectMother.oppgaveId(),
        antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
            virkningsperiode,
        ),
        avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
        resultat: SøknadsbehandlingType = SøknadsbehandlingType.INNVILGELSE,
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
                SøknadsbehandlingType.INNVILGELSE -> OppdaterSøknadsbehandlingKommando.Innvilgelse(
                    sakId = sakId,
                    saksbehandler = saksbehandler,
                    behandlingId = id,
                    correlationId = CorrelationId.generate(),
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                    begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                    innvilgelsesperiode = virkningsperiode,
                    barnetillegg = barnetillegg,
                    tiltaksdeltakelser = valgteTiltaksdeltakelser,
                    antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
                    automatiskSaksbehandlet = automatiskBehandling,
                )

                SøknadsbehandlingType.AVSLAG -> OppdaterSøknadsbehandlingKommando.Avslag(
                    sakId = sakId,
                    saksbehandler = saksbehandler,
                    behandlingId = id,
                    correlationId = CorrelationId.generate(),
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                    begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                    avslagsgrunner = avslagsgrunner!!,
                    automatiskSaksbehandlet = automatiskBehandling,
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
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev.createOrThrow("nySøknadsbehandlingKlarTilBeslutning()"),
        begrunnelseVilkårsvurdering: Begrunnelse = Begrunnelse.createOrThrow("nySøknadsbehandlingKlarTilBeslutning()"),
        virkningsperiode: Periode = virkningsperiode(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(virkningsperiode),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = virkningsperiode.fraOgMed,
            tom = virkningsperiode.tilOgMed,
        ),
        valgteTiltaksdeltakelser: List<Pair<Periode, String>> = saksopplysninger.tiltaksdeltakelser.map {
            Pair(virkningsperiode, it.eksternDeltakelseId)
        },
        oppgaveId: OppgaveId = ObjectMother.oppgaveId(),
        antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
            virkningsperiode,
        ),
        avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
        resultat: SøknadsbehandlingType = SøknadsbehandlingType.INNVILGELSE,
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
            virkningsperiode = virkningsperiode,
            saksopplysninger = saksopplysninger,
            valgteTiltaksdeltakelser = valgteTiltaksdeltakelser,
            oppgaveId = oppgaveId,
            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
            avslagsgrunner = avslagsgrunner,
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
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev.createOrThrow("nyBehandlingUnderBeslutning()"),
        begrunnelseVilkårsvurdering: Begrunnelse = Begrunnelse.createOrThrow("nyBehandlingUnderBeslutning()"),
        virkningsperiode: Periode = virkningsperiode(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(virkningsperiode),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = virkningsperiode.fraOgMed,
            tom = virkningsperiode.tilOgMed,
        ),
        valgteTiltaksdeltakelser: List<Pair<Periode, String>> = saksopplysninger.tiltaksdeltakelser.map {
            Pair(virkningsperiode, it.eksternDeltakelseId)
        },
        antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
            virkningsperiode,
        ),
        oppgaveId: OppgaveId = ObjectMother.oppgaveId(),
        resultat: SøknadsbehandlingType = SøknadsbehandlingType.INNVILGELSE,
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
            virkningsperiode = virkningsperiode,
            saksopplysninger = saksopplysninger,
            valgteTiltaksdeltakelser = valgteTiltaksdeltakelser,
            oppgaveId = oppgaveId,
            resultat = resultat,
            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
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
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev.createOrThrow("nyBehandlingUnderBeslutning()"),
        begrunnelseVilkårsvurdering: Begrunnelse = Begrunnelse.createOrThrow("nyBehandlingUnderBeslutning()"),
        virkningsperiode: Periode = virkningsperiode(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(virkningsperiode),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = virkningsperiode.fraOgMed,
            tom = virkningsperiode.tilOgMed,
        ),
        valgteTiltaksdeltakelser: List<Pair<Periode, String>> = saksopplysninger.tiltaksdeltakelser.map {
            Pair(virkningsperiode, it.eksternDeltakelseId)
        },
        oppgaveId: OppgaveId = ObjectMother.oppgaveId(),
        utdøvendeBeslutter: Saksbehandler = beslutter(),
        resultat: SøknadsbehandlingType = SøknadsbehandlingType.INNVILGELSE,
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
            virkningsperiode = virkningsperiode,
            saksopplysninger = saksopplysninger,
            valgteTiltaksdeltakelser = valgteTiltaksdeltakelser,
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

    fun nyVedtattSøknadsbehandling(
        id: BehandlingId = BehandlingId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(1.januar(2024), "1234"),
        fnr: Fnr = Fnr.random(),
        saksbehandler: Saksbehandler = saksbehandler(),
        sendtTilBeslutning: LocalDateTime? = null,
        søknad: InnvilgbarSøknad = nyInnvilgbarSøknad(),
        beslutter: Saksbehandler = beslutter(),
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev.createOrThrow("nyBehandlingUnderBeslutning()"),
        begrunnelseVilkårsvurdering: Begrunnelse = Begrunnelse.createOrThrow("nyBehandlingUnderBeslutning()"),
        virkningsperiode: Periode = virkningsperiode(),
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(virkningsperiode),
        saksopplysninger: Saksopplysninger = saksopplysninger(
            fom = virkningsperiode.fraOgMed,
            tom = virkningsperiode.tilOgMed,
        ),
        valgteTiltaksdeltakelser: List<Pair<Periode, String>> = saksopplysninger.tiltaksdeltakelser.map {
            Pair(virkningsperiode, it.eksternDeltakelseId)
        },
        oppgaveId: OppgaveId = ObjectMother.oppgaveId(),
        resultat: SøknadsbehandlingType = SøknadsbehandlingType.INNVILGELSE,
        clock: Clock = fixedClock,
        antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
            AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
            virkningsperiode,
        ),
        avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
        omgjørRammevedtak: OmgjørRammevedtak = OmgjørRammevedtak.empty,
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
            virkningsperiode = virkningsperiode,
            saksopplysninger = saksopplysninger,
            valgteTiltaksdeltakelser = valgteTiltaksdeltakelser,
            oppgaveId = oppgaveId,
            resultat = resultat,
            clock = clock,
            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
            avslagsgrunner = avslagsgrunner,
            omgjørRammevedtak = omgjørRammevedtak,
        ).iverksett(
            utøvendeBeslutter = beslutter,
            attestering = godkjentAttestering(beslutter, clock),
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
            begrunnelse = begrunnelse,
            tidspunkt = tidspunkt,
        )
    }
}

fun TestApplicationContext.nyInnvilgbarSøknad(
    periode: Periode = ObjectMother.virkningsperiode(),
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
    periode: Periode = ObjectMother.virkningsperiode(),
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
    tiltaksdeltakelse: Tiltaksdeltakelse? = null,
    søknadstiltak: Søknadstiltak? = tiltaksdeltakelse?.toSøknadstiltak(),
    sak: Sak = ObjectMother.nySak(fnr = fnr),
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
        tiltaksdeltakelse = tiltaksdeltakelse,
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
        this.behandlingContext.behandlingRepo.lagre(it)
    }
    return sak.leggTilSøknadsbehandling(behandlingUnderBehandling) to behandlingUnderBehandling
}

suspend fun TestApplicationContext.søknadsbehandlingTilBeslutter(
    periode: Periode = ObjectMother.virkningsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    correlationId: CorrelationId = CorrelationId.generate(),
    fritekstTilVedtaksbrev: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev.createOrThrow("Fritekst"),
    begrunnelseVilkårsvurdering: Begrunnelse = Begrunnelse.createOrThrow("Begrunnelse"),
    antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
        AntallDagerForMeldeperiode((DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE)),
        periode,
    ),
    avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
    barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(periode),
    resultat: SøknadsbehandlingType,
    automatiskBehandling: Boolean = false,
): Sak {
    val (sakMedSøknadsbehandling) = startSøknadsbehandling(
        periode = periode,
        fnr = fnr,
    )
    val behandling = sakMedSøknadsbehandling.rammebehandlinger.singleOrNullOrThrow()!! as Søknadsbehandling
    val tiltaksdeltakelser = listOf(
        Pair(
            periode,
            behandling.saksopplysninger.tiltaksdeltakelser.first().eksternDeltakelseId,
        ),
    )

    this.behandlingContext.oppdaterBehandlingService.oppdater(
        when (resultat) {
            SøknadsbehandlingType.INNVILGELSE -> OppdaterSøknadsbehandlingKommando.Innvilgelse(
                sakId = sakMedSøknadsbehandling.id,
                behandlingId = behandling.id,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                innvilgelsesperiode = periode,
                barnetillegg = barnetillegg,
                tiltaksdeltakelser = tiltaksdeltakelser,
                antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
                automatiskSaksbehandlet = automatiskBehandling,
            )

            SøknadsbehandlingType.AVSLAG -> OppdaterSøknadsbehandlingKommando.Avslag(
                sakId = sakMedSøknadsbehandling.id,
                behandlingId = behandling.id,
                saksbehandler = saksbehandler,
                correlationId = correlationId,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
                avslagsgrunner = avslagsgrunner!!,
                automatiskSaksbehandlet = automatiskBehandling,
            )
        },
    ).getOrFail()

    this.behandlingContext.sendBehandlingTilBeslutningService.sendTilBeslutning(
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
    periode: Periode = ObjectMother.virkningsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
    resultat: SøknadsbehandlingType,
    antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
        AntallDagerForMeldeperiode((DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE)),
        periode,
    ),
    barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(periode),
    avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
): Sak {
    val vilkårsvurdert = søknadsbehandlingTilBeslutter(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        resultat = resultat,
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
        barnetillegg = barnetillegg,
        avslagsgrunner = avslagsgrunner,
    )
    this.behandlingContext.taBehandlingService.taBehandling(
        vilkårsvurdert.id,
        vilkårsvurdert.rammebehandlinger.singleOrNullOrThrow()!!.id,
        beslutter,
    )
    return this.sakContext.sakService.hentForSakId(
        vilkårsvurdert.id,
    )
}

suspend fun TestApplicationContext.søknadssbehandlingIverksatt(
    periode: Periode = ObjectMother.virkningsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
    resultat: SøknadsbehandlingType,
    antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
        AntallDagerForMeldeperiode((DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE)),
        periode,
    ),
    barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(periode),
    avslagsgrunner: NonEmptySet<Avslagsgrunnlag>? = null,
): Pair<Sak, Søknadsbehandling> {
    val tac = this
    val underBeslutning = søknadsbehandlingUnderBeslutning(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        resultat = resultat,
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
        barnetillegg = barnetillegg,
        avslagsgrunner = avslagsgrunner,
    )
    val behandling = tac.behandlingContext.iverksettBehandlingService.iverksettRammebehandling(
        behandlingId = underBeslutning.rammebehandlinger.singleOrNullOrThrow()!!.id,
        beslutter = beslutter,
        sakId = underBeslutning.id,
    ).getOrFail().second

    return this.sakContext.sakService.hentForSakId(behandling.sakId) to behandling as Søknadsbehandling
}

suspend fun TestApplicationContext.søknadsbehandlingIverksattMedMeldeperioder(
    periode: Periode = ObjectMother.virkningsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
    clock: Clock = fixedClock,
    resultat: SøknadsbehandlingType = SøknadsbehandlingType.INNVILGELSE,
    antallDagerPerMeldeperiode: SammenhengendePeriodisering<AntallDagerForMeldeperiode> = SammenhengendePeriodisering(
        AntallDagerForMeldeperiode((DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE)),
        periode,
    ),
    barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(periode),
): Sak {
    val (sak, meldeperioder) = søknadssbehandlingIverksatt(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        resultat = resultat,
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
        barnetillegg = barnetillegg,
    ).first.genererMeldeperioder(clock)

    this.meldekortContext.meldeperiodeRepo.lagre(meldeperioder)

    return sak
}

/**
 * Oppretter sak, søknad, iverksetter søknadsbehandling og oppretter meldekortbehandling
 */
suspend fun TestApplicationContext.meldekortBehandlingOpprettet(
    innvilgelsesperiode: Periode = ObjectMother.virkningsperiode(),
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
        resultat = SøknadsbehandlingType.INNVILGELSE,
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
 * Oppretter sak, søknad, iverksetter søknadsbehandling, oppretter meldekortbehandling og sender den til beslutter
 */
suspend fun TestApplicationContext.meldekortTilBeslutter(
    innvilgelsesperiode: Periode = ObjectMother.virkningsperiode(),
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
    tac.meldekortContext.sendMeldekortTilBeslutterService.sendMeldekortTilBeslutter(
        sak.meldekortbehandlinger.first().tilSendMeldekortTilBeslutterKommando(
            saksbehandler,
        ),
    )
    return this.sakContext.sakService.hentForSakId(sak.id)
}

/**
 * Oppretter sak, søknad, iverksetter søknadsbehandling, oppretter og iverksetter første meldekortbehandling.
 *
 * Genererer og sender også utbetaling for meldekortet
 */
suspend fun TestApplicationContext.førsteMeldekortIverksatt(
    innvilgelsesperiode: Periode = ObjectMother.virkningsperiode(),
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
    tac.utbetalingContext.sendUtbetalingerService.send()
    return this.sakContext.sakService.hentForSakId(sak.id)
}

suspend fun TestApplicationContext.andreMeldekortOpprettet(
    periode: Periode = ObjectMother.virkningsperiode(),
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

    tac.meldekortContext.opprettMeldekortBehandlingService.opprettBehandling(
        sakId = sak.id,
        kjedeId = sak.meldeperiodeKjeder[1].kjedeId,
        saksbehandler = saksbehandler,
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
