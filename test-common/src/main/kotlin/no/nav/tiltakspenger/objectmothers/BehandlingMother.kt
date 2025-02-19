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
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.IverksettMeldekortKommando
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.objectmothers.ObjectMother.personSøknad
import no.nav.tiltakspenger.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.objectmothers.ObjectMother.saksbehandler123
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Attestering
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Attesteringsstatus
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknad
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.domene.personopplysninger.PersonopplysningerSøker
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold.LeggTilLivsoppholdSaksopplysningCommand
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold.leggTilLivsoppholdSaksopplysning
import java.time.LocalDate
import java.time.LocalDateTime

interface BehandlingMother {
    /** Felles default vurderingsperiode for testdatatypene */
    fun vurderingsperiode() = Periode(1.januar(2023), 31.mars(2023))
    fun revurderingsperiode() = Periode(2.januar(2023), 31.mars(2023))

    /**
     * Dette er kun det første steget i en behandlingsprosess. Bruk andre helper-funksjoner for å starte i en senere tilstand.
     */
    fun behandlingUnderBehandlingUavklart(
        periode: Periode = vurderingsperiode(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer("202301011001"),
        fnr: Fnr = Fnr.random(),
        søknad: Søknad = ObjectMother.nySøknad(periode = periode),
        personopplysningFødselsdato: LocalDate = 1.januar(2000),
        registrerteTiltak: List<Tiltaksdeltagelse> = listOf(søknad.tiltak.toTiltak()),
        saksbehandler: Saksbehandler = saksbehandler(),
        saksopplysninger: Saksopplysninger = Saksopplysninger(
            fødselsdato = personopplysningFødselsdato,
            tiltaksdeltagelse = registrerteTiltak.single(),
        ),
    ): Behandling = runBlocking {
        Behandling.opprettDeprecatedFørstegangsbehandling(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            søknad = søknad,
            saksbehandler = saksbehandler,
            hentSaksopplysninger = { saksopplysninger },
        ).getOrNull()!!
    }

    fun behandlingUnderBehandlingInnvilget(
        vurderingsperiode: Periode = vurderingsperiode(),
        sakId: SakId = SakId.random(),
        søknad: Søknad = ObjectMother.nySøknad(periode = vurderingsperiode),
        saksbehandler: Saksbehandler = saksbehandler(),
        correlationId: CorrelationId = CorrelationId.generate(),
        behandling: Behandling = behandlingUnderBehandlingUavklart(
            periode = vurderingsperiode,
            sakId = sakId,
            søknad = søknad,
            saksbehandler = saksbehandler,
        ),
        livsoppholdCommand: LeggTilLivsoppholdSaksopplysningCommand = LeggTilLivsoppholdSaksopplysningCommand(
            behandlingId = behandling.id,
            saksbehandler = saksbehandler,
            harYtelseForPeriode = LeggTilLivsoppholdSaksopplysningCommand.HarYtelseForPeriode(
                periode = vurderingsperiode,
                harYtelse = false,
            ),
            correlationId = correlationId,
        ),
    ): Behandling = behandlingUnderBehandlingUavklart(
        periode = vurderingsperiode,
        sakId = sakId,
        søknad = søknad,
        saksbehandler = saksbehandler,
    ).leggTilLivsoppholdSaksopplysning(
        command = livsoppholdCommand,
    ).getOrNull()!!

    fun behandlingUnderBehandlingAvslag(
        periode: Periode = vurderingsperiode(),
        sakId: SakId = SakId.random(),
        søknad: Søknad = ObjectMother.nySøknad(periode = periode),
        saksbehandler: Saksbehandler = saksbehandler(),
        correlationId: CorrelationId = CorrelationId.generate(),
    ): Behandling {
        val behandling = behandlingUnderBehandlingUavklart(
            periode = periode,
            sakId = sakId,
            søknad = søknad,
            saksbehandler = saksbehandler,
        )

        behandling.vilkårssett!!.oppdaterLivsopphold(
            LeggTilLivsoppholdSaksopplysningCommand(
                behandlingId = behandling.id,
                saksbehandler = saksbehandler,
                harYtelseForPeriode = LeggTilLivsoppholdSaksopplysningCommand.HarYtelseForPeriode(
                    periode = behandling.vurderingsperiode!!,
                    harYtelse = true,
                ),
                correlationId = correlationId,
            ),
        )

        return behandling
    }

    fun godkjentAttestering(beslutter: Saksbehandler = beslutter()): Attestering = Attestering(
        id = AttesteringId.random(),
        status = Attesteringsstatus.GODKJENT,
        begrunnelse = null,
        beslutter = beslutter.navIdent,
        tidspunkt = nå(),
    )

    fun behandlingTilBeslutterInnvilget(saksbehandler: Saksbehandler = saksbehandler123()): Behandling {
        val behandling = behandlingUnderBehandlingInnvilget(saksbehandler = saksbehandler)
        return behandling.tilBeslutning(saksbehandler)
    }

    fun behandlingTilBeslutterAvslag(): Behandling =
        behandlingUnderBehandlingAvslag().copy(saksbehandler = saksbehandler123().navIdent)
            .tilBeslutning(saksbehandler123())

    fun behandlingInnvilgetIverksatt(): Behandling =
        behandlingTilBeslutterInnvilget(saksbehandler123()).copy(beslutter = beslutter().navIdent)
            .iverksett(beslutter(), godkjentAttestering())
}

suspend fun TestApplicationContext.nySøknad(
    periode: Periode = ObjectMother.vurderingsperiode(),
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
 * @param søknad Dersom du sender inn denne, bør du og sende inn tiltak+fnr for at de skal henge sammen.
 */
suspend fun TestApplicationContext.førstegangsbehandlingUavklart(
    periode: Periode = ObjectMother.vurderingsperiode(),
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
    return this.behandlingContext.behandlingService.startFørstegangsbehandling(
        søknad.id,
        sak.id,
        saksbehandler,
        correlationId = CorrelationId.generate(),
    ).getOrFail()
}

suspend fun TestApplicationContext.førstegangsbehandlingVilkårsvurdert(
    periode: Periode = ObjectMother.vurderingsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    correlationId: CorrelationId = CorrelationId.generate(),
): Sak {
    val uavklart = førstegangsbehandlingUavklart(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
    )
    this.behandlingContext.livsoppholdVilkårService.leggTilSaksopplysning(
        LeggTilLivsoppholdSaksopplysningCommand(
            behandlingId = uavklart.førstegangsbehandling!!.id,
            saksbehandler = saksbehandler,
            harYtelseForPeriode = LeggTilLivsoppholdSaksopplysningCommand.HarYtelseForPeriode(
                periode = periode,
                harYtelse = false,
            ),
            correlationId = correlationId,
        ),
    )
    return this.sakContext.sakService.hentForSakId(
        uavklart.id,
        saksbehandler,
        correlationId = CorrelationId.generate(),
    ).getOrFail()
}

suspend fun TestApplicationContext.førstegangsbehandlingTilBeslutter(
    periode: Periode = ObjectMother.vurderingsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
): Sak {
    val vilkårsvurdert = førstegangsbehandlingVilkårsvurdert(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
    )

    this.behandlingContext.behandlingService.sendTilBeslutter(
        vilkårsvurdert.førstegangsbehandling!!.id,
        saksbehandler,
        correlationId = CorrelationId.generate(),
    ).getOrFail()
    return this.sakContext.sakService.hentForSakId(
        vilkårsvurdert.id,
        saksbehandler,
        correlationId = CorrelationId.generate(),
    ).getOrFail()
}

suspend fun TestApplicationContext.førstegangsbehandlingUnderBeslutning(
    periode: Periode = ObjectMother.vurderingsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
): Sak {
    val vilkårsvurdert = førstegangsbehandlingTilBeslutter(
        periode = periode,
        fnr = fnr,
        saksbehandler = saksbehandler,
    )
    this.behandlingContext.behandlingService.taBehandling(
        vilkårsvurdert.førstegangsbehandling!!.id,
        beslutter,
        correlationId = CorrelationId.generate(),
    )
    return this.sakContext.sakService.hentForSakId(
        vilkårsvurdert.id,
        saksbehandler,
        correlationId = CorrelationId.generate(),
    ).getOrFail()
}

suspend fun TestApplicationContext.førstegangsbehandlingIverksatt(
    periode: Periode = ObjectMother.vurderingsperiode(),
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
        tac.behandlingContext.behandlingService.iverksett(
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
    periode: Periode = ObjectMother.vurderingsperiode(),
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
    periode: Periode = ObjectMother.vurderingsperiode(),
    fnr: Fnr = Fnr.random(),
    saksbehandler: Saksbehandler = saksbehandler(),
    beslutter: Saksbehandler = beslutter(),
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
    return this.sakContext.sakService.hentForSakId(sak.id, saksbehandler, correlationId = CorrelationId.generate())
        .getOrFail()
}

/**
 * Genererer også utbetalingsvedtak, men sender ikke til utbetaling.
 */
suspend fun TestApplicationContext.førsteMeldekortIverksatt(
    periode: Periode = ObjectMother.vurderingsperiode(),
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
            correlationId = CorrelationId.generate(),
        ),
    )
    // Emulerer at jobben kjører
    tac.utbetalingContext.sendUtbetalingerService.send()
    return this.sakContext.sakService.hentForSakId(sak.id, saksbehandler, correlationId = correlationId).getOrFail()
}

suspend fun TestApplicationContext.andreMeldekortIverksatt(
    periode: Periode = ObjectMother.vurderingsperiode(),
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
