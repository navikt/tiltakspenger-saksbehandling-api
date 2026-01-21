package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningId
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Velferd.FraværAnnet
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Velferd.FraværGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.IkkeBesvart
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.IkkeRettTilTiltakspenger
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningerVedtatt
import no.nav.tiltakspenger.saksbehandling.beregning.beregnMeldekort
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.erHelg
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostIdGeneratorSerial
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtaksliste
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort.BrukersMeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.LagreBrukersMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingType
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDager
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortbehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldekortvedtaksliste
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Dager
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.opprettVedtak
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtaksliste
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

interface MeldekortMother : MotherOfAllMothers {
    @Suppress("unused")
    fun meldekortUnderBehandling(
        id: MeldekortId = MeldekortId.random(),
        sakId: SakId = SakId.random(),
        clock: Clock = fixedClock,
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
        fnr: Fnr = Fnr.random(),
        periode: Periode = Periode(6.januar(2025), 19.januar(2025)),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
        saksbehandler: String? = "saksbehandler",
        beslutter: String = "beslutter",
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        status: MeldekortBehandlingStatus = MeldekortBehandlingStatus.UNDER_BEHANDLING,
        navkontor: Navkontor = ObjectMother.navkontor(),
        opprettet: LocalDateTime = nå(clock),
        antallDagerForPeriode: Int = 10,
        meldeperiode: Meldeperiode = meldeperiode(
            periode = periode,
            kjedeId = kjedeId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            antallDagerForPeriode = antallDagerForPeriode,
        ),
        type: MeldekortBehandlingType = MeldekortBehandlingType.FØRSTE_BEHANDLING,
        attesteringer: Attesteringer = Attesteringer.empty(),
        dager: MeldekortDager = genererMeldekortdagerFraMeldeperiode(meldeperiode),
        sistEndret: LocalDateTime = opprettet,
        behandlingSendtTilDatadeling: LocalDateTime? = null,
    ): MeldekortUnderBehandling {
        return MeldekortUnderBehandling(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            navkontor = navkontor,
            ikkeRettTilTiltakspengerTidspunkt = null,
            meldeperiode = meldeperiode,
            brukersMeldekort = null,
            saksbehandler = saksbehandler,
            type = type,
            attesteringer = attesteringer,
            begrunnelse = null,
            sendtTilBeslutning = null,
            dager = dager,
            beregning = null,
            simulering = null,
            status = status,
            sistEndret = sistEndret,
            behandlingSendtTilDatadeling = behandlingSendtTilDatadeling,
            fritekstTilVedtaksbrev = null,
        )
    }

    fun meldekortBehandletManuelt(
        clock: Clock = this.clock,
        id: MeldekortId = MeldekortId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
        fnr: Fnr = Fnr.random(),
        periode: Periode = Periode(6.januar(2025), 19.januar(2025)),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
        opprettet: LocalDateTime = nå(clock),
        antallDagerForPeriode: Int = 10,
        meldeperiode: Meldeperiode = meldeperiode(
            periode = periode,
            kjedeId = kjedeId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            antallDagerForPeriode = antallDagerForPeriode,
        ),
        barnetilleggsPerioder: Periodisering<AntallBarn>? = null,
        meldekortperiodeBeregning: Beregning =
            meldekortBeregning(
                meldekortId = id,
                startDato = meldeperiode.periode.fraOgMed,
                barnetilleggsPerioder = barnetilleggsPerioder,
            ),
        dager: MeldekortDager = genererMeldekortdagerFraMeldeperiode(meldeperiode),
        saksbehandler: String = "saksbehandler",
        beslutter: String? = "beslutter",
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        status: MeldekortBehandlingStatus = MeldekortBehandlingStatus.GODKJENT,
        iverksattTidspunkt: LocalDateTime? = nå(clock),
        navkontor: Navkontor = ObjectMother.navkontor(),
        sendtTilBeslutning: LocalDateTime = nå(clock),
        erFørsteBehandlingForPerioden: Boolean = true,
        type: MeldekortBehandlingType = MeldekortBehandlingType.FØRSTE_BEHANDLING,
        attesteringer: Attesteringer = Attesteringer.empty(),
        begrunnelse: Begrunnelse? = null,
        simulering: Simulering? = null,
        sistEndret: LocalDateTime = iverksattTidspunkt ?: sendtTilBeslutning,
        behandlingSendtTilDatadeling: LocalDateTime? = null,
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev? = null,
    ): MeldekortBehandletManuelt {
        return MeldekortBehandletManuelt(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            beregning = meldekortperiodeBeregning,
            simulering = simulering,
            saksbehandler = saksbehandler,
            sendtTilBeslutning = sendtTilBeslutning,
            beslutter = beslutter,
            status = status,
            iverksattTidspunkt = iverksattTidspunkt,
            navkontor = navkontor,
            ikkeRettTilTiltakspengerTidspunkt = null,
            meldeperiode = meldeperiode,
            brukersMeldekort = null,
            type = type,
            begrunnelse = begrunnelse,
            attesteringer = attesteringer,
            dager = dager,
            sistEndret = sistEndret,
            behandlingSendtTilDatadeling = behandlingSendtTilDatadeling,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
        )
    }

    fun meldekortBehandletAutomatisk(
        id: MeldekortId = MeldekortId.random(),
        sakId: SakId = SakId.random(),
        clock: Clock = fixedClock,
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
        fnr: Fnr = Fnr.random(),
        opprettet: LocalDateTime = nå(clock),
        barnetilleggsPerioder: Periodisering<AntallBarn>? = null,
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        navkontor: Navkontor = ObjectMother.navkontor(),
        meldeperiode: Meldeperiode = meldeperiode(
            periode = Periode(6.januar(2025), 19.januar(2025)),
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            antallDagerForPeriode = 10,
        ),
        dager: MeldekortDager = genererMeldekortdagerFraMeldeperiode(meldeperiode),
        type: MeldekortBehandlingType = MeldekortBehandlingType.FØRSTE_BEHANDLING,
        brukersMeldekort: BrukersMeldekort = brukersMeldekort(
            sakId = sakId,
            meldeperiode = meldeperiode,
            behandlesAutomatisk = true,
            mottatt = nå(clock),

        ),
        beregning: Beregning = brukersMeldekort.tilMeldekortBeregning(
            meldekortBehandlingId = id,
            barnetilleggsPerioder = barnetilleggsPerioder,
        ),
        simulering: Simulering? = null,
        sistEndret: LocalDateTime = opprettet,
        behandlingSendtTilDatadeling: LocalDateTime? = null,
    ): MeldekortBehandletAutomatisk {
        return MeldekortBehandletAutomatisk(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            navkontor = navkontor,
            brukersMeldekort = brukersMeldekort,
            meldeperiode = meldeperiode,
            dager = dager,
            beregning = beregning,
            type = type,
            status = MeldekortBehandlingStatus.AUTOMATISK_BEHANDLET,
            simulering = simulering,
            sistEndret = sistEndret,
            behandlingSendtTilDatadeling = behandlingSendtTilDatadeling,

        )
    }

    fun Sak.leggTilMeldekortBehandletAutomatisk(
        periode: Periode,
        opprettet: LocalDateTime = nå(clock),
        navkontor: Navkontor = ObjectMother.navkontor(),
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        brukersMeldekort: BrukersMeldekort = brukersMeldekort(
            sakId = id,
            meldeperiode = meldeperiodeKjeder.hentMeldeperiode(periode)!!,
            behandlesAutomatisk = true,
            mottatt = nå(clock),
        ),
    ): Pair<Sak, MeldekortBehandletAutomatisk> {
        val meldekortBehandling = meldekortBehandletAutomatisk(
            sakId = id,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            barnetilleggsPerioder = barnetilleggsperioder,
            navkontor = navkontor,
            meldeperiode = meldeperiodeKjeder.hentMeldeperiode(periode)!!,
            brukersMeldekort = brukersMeldekort,
            tiltakstype = tiltakstype,
        )
        val vedtak = meldekortBehandling.opprettVedtak(
            forrigeUtbetaling = utbetalinger.lastOrNull(),
            clock = clock,
        )

        return this.copy(
            behandlinger = behandlinger.leggTilMeldekortBehandletAutomatisk(meldekortBehandling),
            vedtaksliste = vedtaksliste.leggTilMeldekortvedtak(vedtak),
        ) to meldekortBehandling
    }

    fun BrukersMeldekort.tilMeldekortBeregning(
        meldekortBehandlingId: MeldekortId = MeldekortId.random(),
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        barnetilleggsPerioder: Periodisering<AntallBarn>? = null,
        reduksjon: ReduksjonAvYtelsePåGrunnAvFravær = ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon,
    ): Beregning {
        return Beregning(
            nonEmptyListOf(
                MeldeperiodeBeregning(
                    id = BeregningId.random(),
                    kjedeId = kjedeId,
                    meldekortId = meldekortBehandlingId,
                    beregningKilde = BeregningKilde.BeregningKildeMeldekort(meldekortBehandlingId),
                    dager = tilMeldekortDager().map {
                        val dato = it.dato
                        val antallBarn: AntallBarn by lazy {
                            barnetilleggsPerioder?.hentVerdiForDag(dato) ?: AntallBarn.ZERO
                        }

                        when (it.status) {
                            MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> DeltattUtenLønnITiltaket.create(
                                dato,
                                tiltakstype,
                                antallBarn,
                            )

                            MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> DeltattMedLønnITiltaket.create(
                                dato,
                                tiltakstype,
                                antallBarn,
                            )

                            MeldekortDagStatus.FRAVÆR_SYK -> SykBruker.create(dato, reduksjon, tiltakstype, antallBarn)
                            MeldekortDagStatus.FRAVÆR_SYKT_BARN -> SyktBarn.create(
                                dato,
                                reduksjon,
                                tiltakstype,
                                antallBarn,
                            )

                            MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> FraværGodkjentAvNav.create(
                                dato,
                                tiltakstype,
                                antallBarn,
                            )

                            MeldekortDagStatus.FRAVÆR_ANNET -> FraværAnnet.create(dato, tiltakstype, antallBarn)
                            MeldekortDagStatus.IKKE_BESVART -> IkkeBesvart.create(dato, tiltakstype, antallBarn)
                            MeldekortDagStatus.IKKE_TILTAKSDAG -> IkkeDeltatt.create(dato, tiltakstype, antallBarn)
                            MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> IkkeRettTilTiltakspenger(dato)
                        }
                    }.toNonEmptyListOrNull()!!,
                ),
            ),
        )
    }

    /**
     * @param startDato Må starte på en mandag.
     */
    fun meldekortBeregning(
        startDato: LocalDate = LocalDate.of(2023, 1, 2),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(
            Periode(startDato, startDato.plusDays(13)),
        ),
        meldekortId: MeldekortId = MeldekortId.random(),
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
        return Beregning(
            nonEmptyListOf(
                MeldeperiodeBeregning(
                    id = BeregningId.random(),
                    kjedeId = kjedeId,
                    meldekortId = meldekortId,
                    beregningKilde = BeregningKilde.BeregningKildeMeldekort(meldekortId),
                    dager = beregningDager,
                ),
            ),
        )
    }

    fun maksAntallDeltattTiltaksdagerIMeldekortperiode(
        startDato: LocalDate,
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett,
        barnetilleggsPerioder: Periodisering<AntallBarn>? = null,
    ): NonEmptyList<MeldeperiodeBeregningDag> {
        return (
            tiltaksdager(
                startDato = startDato,
                meldekortId = meldekortId,
                tiltakstype = tiltakstype,
                barnetilleggsPerioder = barnetilleggsPerioder,
            ) +
                ikkeTiltaksdager(startDato.plusDays(5), meldekortId, 2, tiltakstype) +
                tiltaksdager(
                    startDato.plusDays(7),
                    meldekortId,
                    tiltakstype,
                    barnetilleggsPerioder = barnetilleggsPerioder,
                ) +
                ikkeTiltaksdager(startDato.plusDays(12), meldekortId, 2, tiltakstype)
            ).toNonEmptyListOrNull()!!
    }

    fun tiltaksdager(
        startDato: LocalDate,
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        antallDager: Int = 5,
        barnetilleggsPerioder: Periodisering<AntallBarn>? = null,
    ): NonEmptyList<DeltattUtenLønnITiltaket> {
        require(antallDager in 1..5) {
            "Antall sammenhengende dager vil aldri være mer mindre enn 1 eller mer enn 5, men var $antallDager"
        }
        return List(antallDager) { index ->
            val dato = startDato.plusDays(index.toLong())
            DeltattUtenLønnITiltaket.create(
                dato = dato,
                tiltakstype = tiltakstype,
                antallBarn = barnetilleggsPerioder?.hentVerdiForDag(dato) ?: AntallBarn.ZERO,
            )
        }.toNonEmptyListOrNull()!!
    }

    fun tiltaksdagerMedLønn(
        startDato: LocalDate,
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        antallDager: Int = 5,
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
    ): NonEmptyList<DeltattMedLønnITiltaket> {
        require(antallDager in 1..5) {
            "Antall sammenhengende dager vil aldri være mer mindre enn 1 eller mer enn 5, men var $antallDager"
        }
        return List(antallDager) { index ->
            val dato = startDato.plusDays(index.toLong())
            DeltattMedLønnITiltaket.create(
                dato = dato,
                tiltakstype = tiltakstype,
                antallBarn = barnetilleggsPerioder.hentVerdiForDag(dato) ?: AntallBarn.ZERO,
            )
        }.toNonEmptyListOrNull()!!
    }

    fun ikkeTiltaksdager(
        startDato: LocalDate,
        meldekortId: MeldekortId,
        antallDager: Int = 2,
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
    ): NonEmptyList<IkkeDeltatt> {
        require(antallDager in 1..5) {
            "Antall sammenhengende dager vil aldri være mer mindre enn 1 eller mer enn 5, men var $antallDager"
        }
        return List(antallDager) { index ->
            val dato = startDato.plusDays(index.toLong())
            IkkeDeltatt.create(
                dato = dato,
                tiltakstype = tiltakstype,
                antallBarn = barnetilleggsPerioder.hentVerdiForDag(dato) ?: AntallBarn.ZERO,
            )
        }.toNonEmptyListOrNull()!!
    }

    fun ikkeRettDager(
        startDato: LocalDate,
        meldekortId: MeldekortId,
        antallDager: Int = 2,
    ): NonEmptyList<IkkeRettTilTiltakspenger> {
        require(antallDager in 1..14) {
            "Antall sammenhengende dager i en meldeperiode vil aldri være mer mindre enn 1 eller mer enn 14, men var $antallDager"
        }
        return List(antallDager) { index ->
            val dato = startDato.plusDays(index.toLong())
            IkkeRettTilTiltakspenger(dato = dato)
        }.toNonEmptyListOrNull()!!
    }

    suspend fun beregnMeldekortperioder(
        clock: Clock = TikkendeKlokke(),
        vedtaksperiode: Periode,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        meldeperioder: NonEmptyList<NonEmptyList<Dager.Dag>>,
        navkontor: Navkontor = ObjectMother.navkontor(),
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
        begrunnelse: Begrunnelse? = null,
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev? = null,
    ): Meldekortbehandlinger {
        val kommandoer = meldeperioder.map { meldeperiode ->
            OppdaterMeldekortKommando(
                sakId = sakId,
                meldekortId = MeldekortId.random(),
                saksbehandler = saksbehandler,
                dager = Dager(meldeperiode),
                correlationId = CorrelationId.generate(),
                begrunnelse = begrunnelse,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
            )
        }

        val opprettet = nå(clock)

        return kommandoer.drop(1).foldIndexed(
            førsteBeregnetMeldekort(
                clock = clock,
                vedtaksperiode = vedtaksperiode,
                meldekortId = kommandoer.first().meldekortId,
                sakId = sakId,
                fnr = fnr,
                kommando = kommandoer.first(),
                kjedeId = MeldeperiodeKjedeId.fraPeriode(kommandoer.first().periode),
                beslutter = beslutter,
                opprettet = opprettet,
            ).first,
        ) { index, meldekortperioder, kommando ->
            meldekortperioder.beregnNesteMeldekort(
                clock = clock,
                kommando = kommando,
                fnr = fnr,
                vedtaksperiode = vedtaksperiode,
                barnetilleggsPerioder = barnetilleggsPerioder,
                beslutter = beslutter,
                opprettet = opprettet.plusMinutes(1 + index.toLong()),
            )
        }
    }

    suspend fun førsteBeregnetMeldekort(
        kommando: OppdaterMeldekortKommando,
        vedtaksperiode: Periode,
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett> = SammenhengendePeriodisering(
            TiltakstypeSomGirRett.GRUPPE_AMO,
            vedtaksperiode,
        ),
        meldekortId: MeldekortId,
        sakId: SakId,
        clock: Clock = TikkendeKlokke(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
        fnr: Fnr = Fnr.random(),
        opprettet: LocalDateTime = nå(clock),
        status: MeldekortBehandlingStatus = MeldekortBehandlingStatus.UNDER_BEHANDLING,
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(kommando.periode),
        navkontor: Navkontor = ObjectMother.navkontor(),
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
        girRett: Map<LocalDate, Boolean> = kommando.dager.dager.map { it.dag to it.status.girRett() }.toMap(),
        antallDagerForPeriode: Int = girRett.count { it.value },
        begrunnelse: Begrunnelse? = null,
        attesteringer: Attesteringer = Attesteringer.empty(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        simulering: Simulering? = null,
        sistEndret: LocalDateTime = opprettet,
        behandlingSendtTilDatadeling: LocalDateTime? = null,
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev? = null,
    ): Pair<Meldekortbehandlinger, MeldekortBehandletManuelt> {
        val meldeperiode = meldeperiode(
            periode = kommando.periode,
            kjedeId = kjedeId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            girRett = girRett,
            antallDagerForPeriode = antallDagerForPeriode,
        )

        val dager = kommando.dager.tilMeldekortDager(meldeperiode)
        val meldekortBehandlinger = Meldekortbehandlinger(
            verdi = nonEmptyListOf(
                MeldekortUnderBehandling(
                    id = meldekortId,
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    opprettet = opprettet,
                    navkontor = navkontor,
                    beregning = null,
                    ikkeRettTilTiltakspengerTidspunkt = null,
                    meldeperiode = meldeperiode,
                    brukersMeldekort = null,
                    saksbehandler = kommando.saksbehandler.navIdent,
                    type = MeldekortBehandlingType.FØRSTE_BEHANDLING,
                    begrunnelse = begrunnelse,
                    attesteringer = attesteringer,
                    sendtTilBeslutning = null,
                    dager = dager,
                    simulering = simulering,
                    status = status,
                    sistEndret = sistEndret,
                    behandlingSendtTilDatadeling = behandlingSendtTilDatadeling,
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                ),
            ),
        )

        val (oppadatertMeldekortbehandlinger, _) = meldekortBehandlinger.oppdaterMeldekort(
            kommando = kommando,
            clock = clock,
            beregn = {
                beregnMeldekort(
                    meldekortIdSomBeregnes = meldekortId,
                    meldeperiodeSomBeregnes = dager,
                    barnetilleggsPerioder = barnetilleggsPerioder,
                    tiltakstypePerioder = tiltakstypePerioder,
                    meldeperiodeBeregninger = meldekortBehandlinger.tilMeldeperiodeBeregninger(clock),
                )
            },
            simuler = {
                ObjectMother.simuleringMedMetadata(
                    simulering = ObjectMother.simulering(
                        periode = it.periode,
                        meldeperiodeKjedeId = it.kjedeId,
                        meldeperiode = it.meldeperiode,
                        clock = clock,
                    ),
                    originalJson = "{}",
                ).right()
            },
        ).getOrFail()

        return oppadatertMeldekortbehandlinger.sendTilBeslutter(
            kommando = kommando.tilSendMeldekortTilBeslutterKommando(),
            clock = clock,
        )
            .map { (meldekortBehandlinger, meldekort) ->
                val tildeltMeldekort = meldekort.taMeldekortBehandling(beslutter, clock) as MeldekortBehandletManuelt
                val iverksattMeldekort = tildeltMeldekort.iverksettMeldekort(beslutter, clock).getOrFail()
                val oppdaterteBehandlinger = meldekortBehandlinger.oppdaterMeldekortbehandling(iverksattMeldekort)
                Pair(oppdaterteBehandlinger, iverksattMeldekort)
            }
            .getOrFail()
    }

    suspend fun Meldekortbehandlinger.beregnNesteMeldekort(
        kommando: OppdaterMeldekortKommando,
        vedtaksperiode: Periode,
        fnr: Fnr,
        clock: Clock = TikkendeKlokke(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(kommando.periode),
        navkontor: Navkontor = ObjectMother.navkontor(),
        opprettet: LocalDateTime = nå(clock),
        barnetilleggsPerioder: Periodisering<AntallBarn>,
        status: MeldekortBehandlingStatus = MeldekortBehandlingStatus.UNDER_BEHANDLING,
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett> = SammenhengendePeriodisering(
            TiltakstypeSomGirRett.GRUPPE_AMO,
            vedtaksperiode,
        ),
        girRett: Map<LocalDate, Boolean> = kommando.dager.dager.map { it.dag to it.status.girRett() }.toMap(),
        antallDagerForPeriode: Int = girRett.count { it.value },
        attesteringer: Attesteringer = Attesteringer.empty(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        sistEndret: LocalDateTime = opprettet,
        behandlingSendtTilDatadeling: LocalDateTime? = null,
    ): Meldekortbehandlinger {
        val meldekortId = kommando.meldekortId
        val sakId = kommando.sakId
        val meldeperiode = meldeperiode(
            periode = kommando.periode,
            kjedeId = kjedeId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            girRett = girRett,
            antallDagerForPeriode = antallDagerForPeriode,
        )

        val dager = kommando.dager.tilMeldekortDager(meldeperiode)
        val meldekortBehandlinger = this.leggTil(
            MeldekortUnderBehandling(
                id = meldekortId,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                opprettet = opprettet,
                navkontor = navkontor,
                ikkeRettTilTiltakspengerTidspunkt = null,
                meldeperiode = meldeperiode,
                brukersMeldekort = null,
                saksbehandler = kommando.saksbehandler.navIdent,
                type = MeldekortBehandlingType.FØRSTE_BEHANDLING,
                begrunnelse = null,
                attesteringer = attesteringer,
                sendtTilBeslutning = null,
                dager = dager,
                beregning = null,
                simulering = null,
                status = status,
                sistEndret = sistEndret,
                behandlingSendtTilDatadeling = behandlingSendtTilDatadeling,
                fritekstTilVedtaksbrev = null,
            ),
        )

        val (oppadatertMeldekortbehandlinger, _) = meldekortBehandlinger.oppdaterMeldekort(
            kommando = kommando,
            clock = clock,
            beregn = {
                beregnMeldekort(
                    meldekortIdSomBeregnes = meldekortId,
                    meldeperiodeSomBeregnes = dager,
                    barnetilleggsPerioder = barnetilleggsPerioder,
                    tiltakstypePerioder = tiltakstypePerioder,
                    meldeperiodeBeregninger = meldekortBehandlinger.tilMeldeperiodeBeregninger(clock),
                )
            },
            simuler = {
                ObjectMother.simuleringMedMetadata(
                    simulering = ObjectMother.simulering(
                        periode = it.periode,
                        meldeperiodeKjedeId = it.kjedeId,
                        meldeperiode = it.meldeperiode,
                        clock = clock,
                    ),
                    originalJson = "{}",
                ).right()
            },
        ).getOrFail()

        return oppadatertMeldekortbehandlinger.sendTilBeslutter(
            kommando = kommando.tilSendMeldekortTilBeslutterKommando(),
            clock,
        ).map { (meldekortBehandlinger, meldekort) ->
            val tildeltMeldekort = meldekort.taMeldekortBehandling(beslutter, clock) as MeldekortBehandletManuelt
            val iverksattMeldekort = tildeltMeldekort.iverksettMeldekort(beslutter, clock).getOrFail()
            val oppdaterteBehandlinger = meldekortBehandlinger.oppdaterMeldekortbehandling(iverksattMeldekort)
            Pair(oppdaterteBehandlinger, iverksattMeldekort)
        }.getOrFail().first
    }

    fun meldeperiode(
        id: MeldeperiodeId = MeldeperiodeId.random(),
        periode: Periode = Periode(6.januar(2025), 19.januar(2025)),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
        sakId: SakId = SakId.random(),
        versjon: HendelseVersjon = HendelseVersjon.ny(),
        clock: Clock = fixedClock,
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001", clock = clock),
        fnr: Fnr = Fnr.random(),
        opprettet: LocalDateTime = nå(clock),
        antallDagerForPeriode: Int = 10,
        girRettIHelg: Boolean = false,
        girRett: Map<LocalDate, Boolean> = buildMap {
            val perUke = ceil(antallDagerForPeriode / 2.0).toInt()
            val helg = 7 - perUke
            (0 until perUke).forEach { day ->
                put(periode.fraOgMed.plusDays(day.toLong()), true)
            }
            (perUke until 7).forEach { day ->
                put(periode.fraOgMed.plusDays(day.toLong()), girRettIHelg)
            }
            (7 until 14).forEach { day ->
                put(periode.fraOgMed.plusDays(day.toLong()), true)
            }
            ((14 - helg) until 14).forEach { day ->
                put(periode.fraOgMed.plusDays(day.toLong()), girRettIHelg)
            }
        },
        rammevedtak: IkkeTomPeriodisering<VedtakId> = SammenhengendePeriodisering(VedtakId.random(), periode),
    ): Meldeperiode = Meldeperiode(
        kjedeId = kjedeId,
        id = id,
        versjon = versjon,
        sakId = sakId,
        saksnummer = saksnummer,
        fnr = fnr,
        opprettet = opprettet,
        periode = periode,
        maksAntallDagerForMeldeperiode = antallDagerForPeriode,
        girRett = girRett,
        rammevedtak = rammevedtak,
    )

    /** @param meldeperiode Hvis denne sendes inn, bør [sakId] og [mottatt] også sendes inn. */
    @Suppress("unused")
    fun brukersMeldekort(
        id: MeldekortId = MeldekortId.random(),
        clock: Clock = KlokkeMother.clock,
        mottatt: LocalDateTime = nå(clock),
        sakId: SakId = SakId.random(),
        meldeperiode: Meldeperiode = meldeperiode(
            sakId = sakId,
            // Meldeperioden kommer før innsendingen.
            opprettet = mottatt.minus(1, ChronoUnit.MILLIS),
        ),
        dager: List<BrukersMeldekortDag> = meldeperiode.girRett.entries.map { (dato, girRett) ->
            BrukersMeldekortDag(
                status = if (dato.erHelg() || !girRett) InnmeldtStatus.IKKE_BESVART else InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                dato = dato,
            )
        },
        behandlesAutomatisk: Boolean = false,
        behandletAutomatiskStatus: MeldekortBehandletAutomatiskStatus =
            if (behandlesAutomatisk) {
                MeldekortBehandletAutomatiskStatus.VENTER_BEHANDLING
            } else {
                MeldekortBehandletAutomatiskStatus.SKAL_IKKE_BEHANDLES_AUTOMATISK
            },
    ): BrukersMeldekort {
        return BrukersMeldekort(
            id = id,
            mottatt = mottatt,
            meldeperiode = meldeperiode,
            sakId = sakId,
            dager = dager,
            journalpostId = JournalpostIdGeneratorSerial().neste(),
            oppgaveId = null,
            behandlesAutomatisk = behandlesAutomatisk,
            behandletAutomatiskStatus = behandletAutomatiskStatus,
        )
    }

    fun lagreBrukersMeldekortKommando(
        id: MeldekortId = MeldekortId.random(),
        mottatt: LocalDateTime = nå(clock),
        sakId: SakId = SakId.random(),
        meldeperiodeId: MeldeperiodeId = MeldeperiodeId.random(),
        periode: Periode,
        dager: List<BrukersMeldekortDag> = buildList {
            val dagerFraPeriode = periode.tilDager()
            require(dagerFraPeriode.size == 14)
            addAll(dagerFraPeriode.take(5).map { BrukersMeldekortDag(InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET, it) })
            addAll(dagerFraPeriode.subList(5, 7).map { BrukersMeldekortDag(InnmeldtStatus.IKKE_BESVART, it) })
            addAll(
                dagerFraPeriode.subList(7, 12)
                    .map { BrukersMeldekortDag(InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET, it) },
            )
            addAll(dagerFraPeriode.subList(12, 14).map { BrukersMeldekortDag(InnmeldtStatus.IKKE_BESVART, it) })
        },
    ): LagreBrukersMeldekortKommando {
        return LagreBrukersMeldekortKommando(
            id = id,
            mottatt = mottatt,
            meldeperiodeId = meldeperiodeId,
            sakId = sakId,
            dager = dager,
            journalpostId = JournalpostIdGeneratorSerial().neste(),
        )
    }

    fun oppdaterMeldekortKommando(
        sakId: SakId = SakId.random(),
        meldekortId: MeldekortId = MeldekortId.random(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        begrunnelse: Begrunnelse? = null,
        correlationId: CorrelationId = CorrelationId.generate(),
        dager: Dager,
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev? = null,
    ): OppdaterMeldekortKommando {
        return OppdaterMeldekortKommando(
            sakId = sakId,
            meldekortId = meldekortId,
            saksbehandler = saksbehandler,
            dager = dager,
            begrunnelse = begrunnelse,
            correlationId = correlationId,
            fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
        )
    }

    fun sendMeldekortTilBeslutterKommando(
        sakId: SakId = SakId.random(),
        meldekortId: MeldekortId = MeldekortId.random(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        begrunnelse: Begrunnelse? = null,
        correlationId: CorrelationId = CorrelationId.generate(),
        dager: Dager,
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev? = null,
    ): SendMeldekortTilBeslutterKommando {
        return SendMeldekortTilBeslutterKommando(
            sakId = sakId,
            meldekortId = meldekortId,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
        )
    }
}

fun MeldekortBehandling.tilOppdaterMeldekortKommando(
    saksbehandler: Saksbehandler,
): OppdaterMeldekortKommando {
    val dager = dager.map { dag ->
        Dager.Dag(
            dag = dag.dato,
            status = when (dag.status) {
                MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> OppdaterMeldekortKommando.Status.IKKE_RETT_TIL_TILTAKSPENGER
                MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> OppdaterMeldekortKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
                MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> OppdaterMeldekortKommando.Status.DELTATT_MED_LØNN_I_TILTAKET
                MeldekortDagStatus.IKKE_TILTAKSDAG -> OppdaterMeldekortKommando.Status.IKKE_TILTAKSDAG
                MeldekortDagStatus.FRAVÆR_SYK -> OppdaterMeldekortKommando.Status.FRAVÆR_SYK
                MeldekortDagStatus.FRAVÆR_SYKT_BARN -> OppdaterMeldekortKommando.Status.FRAVÆR_SYKT_BARN
                MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> OppdaterMeldekortKommando.Status.FRAVÆR_GODKJENT_AV_NAV
                MeldekortDagStatus.FRAVÆR_ANNET -> OppdaterMeldekortKommando.Status.FRAVÆR_ANNET
                MeldekortDagStatus.IKKE_BESVART -> if (dag.dato.erHelg()) {
                    OppdaterMeldekortKommando.Status.IKKE_TILTAKSDAG
                } else {
                    OppdaterMeldekortKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
                }
            },
        )
    }.toNonEmptyListOrNull()!!

    return ObjectMother.oppdaterMeldekortKommando(
        sakId = sakId,
        meldekortId = id,
        saksbehandler = saksbehandler,
        dager = Dager(dager),
    )
}

fun MeldekortBehandling.tilSendMeldekortTilBeslutterKommando(
    saksbehandler: Saksbehandler,
): SendMeldekortTilBeslutterKommando {
    val dager = dager.map { dag ->
        Dager.Dag(
            dag = dag.dato,
            status = when (dag.status) {
                MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> OppdaterMeldekortKommando.Status.IKKE_RETT_TIL_TILTAKSPENGER
                MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> OppdaterMeldekortKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
                MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> OppdaterMeldekortKommando.Status.DELTATT_MED_LØNN_I_TILTAKET
                MeldekortDagStatus.IKKE_TILTAKSDAG -> OppdaterMeldekortKommando.Status.IKKE_TILTAKSDAG
                MeldekortDagStatus.FRAVÆR_SYK -> OppdaterMeldekortKommando.Status.FRAVÆR_SYK
                MeldekortDagStatus.FRAVÆR_SYKT_BARN -> OppdaterMeldekortKommando.Status.FRAVÆR_SYKT_BARN
                MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> OppdaterMeldekortKommando.Status.FRAVÆR_GODKJENT_AV_NAV
                MeldekortDagStatus.FRAVÆR_ANNET -> OppdaterMeldekortKommando.Status.FRAVÆR_ANNET
                MeldekortDagStatus.IKKE_BESVART -> if (dag.dato.erHelg()) {
                    OppdaterMeldekortKommando.Status.IKKE_TILTAKSDAG
                } else {
                    OppdaterMeldekortKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
                }
            },
        )
    }.toNonEmptyListOrNull()!!

    return ObjectMother.sendMeldekortTilBeslutterKommando(
        sakId = sakId,
        meldekortId = id,
        saksbehandler = saksbehandler,
        dager = Dager(dager),
    )
}

/**
 * @param meldeperiode Perioden meldekortet skal gjelde for. Må være 14 dager, starte på en mandag og slutte på en søndag.
 * @return Meldekortdager for meldeperioden
 * @throws IllegalStateException Dersom alle dagene i en meldekortperiode er IKKE_RETT_TIL_TILTAKSPENGER er den per definisjon utfylt. Dette har vi ikke støtte for i MVP.
 */
private fun genererMeldekortdagerFraMeldeperiode(
    meldeperiode: Meldeperiode,
): MeldekortDager {
    val dager = meldeperiode.girRett.entries.map { (dato, girRett) ->
        MeldekortDag(
            dato = dato,
            status = if (girRett) MeldekortDagStatus.IKKE_BESVART else MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER,
        )
    }

    return if (dager.any { it.status == MeldekortDagStatus.IKKE_BESVART }) {
        MeldekortDager(dager, meldeperiode)
    } else {
        throw IllegalStateException("Alle dagene i en meldekortperiode er IKKE_RETT_TIL_TILTAKSPENGER. Dette har vi ikke støtte for i MVP.")
    }
}

fun OppdaterMeldekortKommando.tilSendMeldekortTilBeslutterKommando(): SendMeldekortTilBeslutterKommando {
    return SendMeldekortTilBeslutterKommando(
        sakId = sakId,
        meldekortId = meldekortId,
        saksbehandler = saksbehandler,
        correlationId = correlationId,
    )
}

fun saksbehandlerFyllerUtMeldeperiodeDager(meldeperiode: Meldeperiode): Dager {
    return Dager(
        dager = buildList {
            val dagerFraPeriode = meldeperiode.periode.tilDager()
            require(dagerFraPeriode.size == 14)
            addAll(
                dagerFraPeriode.take(5)
                    .map { Dager.Dag(it, OppdaterMeldekortKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET) },
            )
            addAll(
                dagerFraPeriode.subList(5, 7).map { Dager.Dag(it, OppdaterMeldekortKommando.Status.IKKE_TILTAKSDAG) },
            )
            addAll(
                dagerFraPeriode.subList(7, 12)
                    .map { Dager.Dag(it, OppdaterMeldekortKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET) },
            )
            addAll(
                dagerFraPeriode.subList(12, 14).map { Dager.Dag(it, OppdaterMeldekortKommando.Status.IKKE_TILTAKSDAG) },
            )
        }.toNonEmptyListOrNull()!!,
    )
}

fun Meldekortbehandlinger.tilMeldeperiodeBeregninger(clock: Clock): MeldeperiodeBeregningerVedtatt {
    return this.sortedBy { it.iverksattTidspunkt }.fold(emptyList<Meldekortvedtak>()) { acc, mkb ->
        if (mkb !is MeldekortBehandling.Behandlet) {
            return@fold acc
        }

        acc.plus(mkb.opprettVedtak(acc.lastOrNull()?.utbetaling, clock))
    }.let {
        MeldeperiodeBeregningerVedtatt.fraVedtaksliste(
            Vedtaksliste(
                Rammevedtaksliste.empty(),
                Meldekortvedtaksliste(it),
                Klagevedtaksliste.empty(),
            ),
        )
    }
}
