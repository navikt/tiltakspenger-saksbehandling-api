package no.nav.tiltakspenger.objectmothers

import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.felles.AttesteringId
import no.nav.tiltakspenger.felles.Systembruker
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.felles.januarDateTime
import no.nav.tiltakspenger.felles.mars
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.IverksettMeldekortKommando
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.objectmothers.ObjectMother.personSøknad
import no.nav.tiltakspenger.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Attestering
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.domene.behandling.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.domene.behandling.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.domene.behandling.SendBehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.domene.personopplysninger.PersonopplysningerSøker
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import java.time.LocalDate
import java.time.LocalDateTime

interface BehandlingMother {
    /** Felles default vurderingsperiode for testdatatypene */
    fun virningsperiode() = Periode(1.januar(2023), 31.mars(2023))
    fun revurderingsperiode() = Periode(2.januar(2023), 31.mars(2023))

    fun godkjentAttestering(beslutter: Saksbehandler = beslutter()): Attestering = Attestering(
        id = AttesteringId.random(),
        status = Attesteringsstatus.GODKJENT,
        begrunnelse = null,
        beslutter = beslutter.navIdent,
        tidspunkt = nå(),
    )
}

suspend fun TestApplicationContext.nySøknad(
    periode: Periode = ObjectMother.virningsperiode(),
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
        sak = sak,
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
    periode: Periode = ObjectMother.virningsperiode(),
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
        sak = sak,
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
    return this.behandlingContext.startSøknadsbehandlingV2Service.startSøknadsbehandling(
        søknad.id,
        sak.id,
        saksbehandler,
        correlationId = correlationId,
    ).getOrFail()
}

suspend fun TestApplicationContext.førstegangsbehandlingTilBeslutter(
    periode: Periode = ObjectMother.virningsperiode(),
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

    this.behandlingContext.sendBehandlingTilBeslutningV2Service.sendFørstegangsbehandlingTilBeslutning(
        SendBehandlingTilBeslutningKommando(
            sakId = sakMedFørstegangsbehandling.id,
            behandlingId = sakMedFørstegangsbehandling.førstegangsbehandling!!.id,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            begrunnelseVilkårsvurdering = begrunnelseVilkårsvurdering,
            innvilgelsesperiode = periode,
        ),
    ).getOrFail()
    return this.sakContext.sakService.hentForSakId(
        sakMedFørstegangsbehandling.id,
        saksbehandler,
        correlationId = correlationId,
    ).getOrFail()
}

suspend fun TestApplicationContext.førstegangsbehandlingUnderBeslutning(
    periode: Periode = ObjectMother.virningsperiode(),
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
    this.behandlingContext.behandlingService.taBehandling(
        vilkårsvurdert.førstegangsbehandling!!.id,
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
    periode: Periode = ObjectMother.virningsperiode(),
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
        tac.behandlingContext.iverksettBehandlingV2Service.iverksett(
            behandlingId = underBeslutning.førstegangsbehandling!!.id,
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
    periode: Periode = ObjectMother.virningsperiode(),
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
        meldeperiodeKjedeId = sak.meldeperiodeKjeder.hentSisteMeldeperiode().meldeperiodeKjedeId,
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
    periode: Periode = ObjectMother.virningsperiode(),
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
        (sak.meldekortBehandlinger.first() as MeldekortBehandling.MeldekortUnderBehandling).tilSendMeldekortTilBeslutterKommando(
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
    periode: Periode = ObjectMother.virningsperiode(),
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
            meldekortId = (sak.meldekortBehandlinger.first() as MeldekortBehandling.MeldekortBehandlet).id,
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
    periode: Periode = ObjectMother.virningsperiode(),
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
        meldeperiodeKjedeId = sak.meldeperiodeKjeder.hentSisteMeldeperiode().meldeperiodeKjedeId,
        saksbehandler = saksbehandler,
        correlationId = correlationId,
    )

    return this.sakContext.sakService.hentForSakId(sak.id, saksbehandler, correlationId = correlationId).getOrFail()
}
