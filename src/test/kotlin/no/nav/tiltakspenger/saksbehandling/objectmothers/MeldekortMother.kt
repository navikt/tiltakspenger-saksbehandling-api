package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.NonEmptyList
import arrow.core.left
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
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Utfallsperiode
import no.nav.tiltakspenger.saksbehandling.felles.erHelg
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostIdGenerator
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort.BrukersMeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.LagreBrukersMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletAutomatisk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandletManuelt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingBegrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingType
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDager
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.OppdaterMeldekortKommando.Dager
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.beregn
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
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
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        fnr: Fnr = Fnr.random(),
        periode: Periode = Periode(6.januar(2025), 19.januar(2025)),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
        saksbehandler: String? = "saksbehandler",
        beslutter: String = "beslutter",
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        status: MeldekortBehandlingStatus = MeldekortBehandlingStatus.GODKJENT,
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
        )
    }

    fun meldekortBehandletManuelt(
        id: MeldekortId = MeldekortId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
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
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
        meldekortperiodeBeregning: MeldekortBeregning =
            meldekortBeregning(
                meldekortId = id,
                sakId = sakId,
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
        begrunnelse: MeldekortBehandlingBegrunnelse? = null,
        simulering: Simulering? = null,
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
        )
    }

    fun meldekortBehandletAutomatisk(
        id: MeldekortId = MeldekortId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        fnr: Fnr = Fnr.random(),
        periode: Periode = Periode(6.januar(2025), 19.januar(2025)),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
        opprettet: LocalDateTime = nå(clock),
        antallDagerForPeriode: Int = 10,
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
        navkontor: Navkontor = ObjectMother.navkontor(),
        meldeperiode: Meldeperiode = meldeperiode(
            periode = periode,
            kjedeId = kjedeId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            antallDagerForPeriode = antallDagerForPeriode,
        ),
        dager: MeldekortDager = genererMeldekortdagerFraMeldeperiode(meldeperiode),
        beregning: MeldekortBeregning =
            meldekortBeregning(
                meldekortId = id,
                sakId = sakId,
                startDato = meldeperiode.periode.fraOgMed,
                barnetilleggsPerioder = barnetilleggsPerioder,
            ),
        type: MeldekortBehandlingType = MeldekortBehandlingType.FØRSTE_BEHANDLING,
        status: MeldekortBehandlingStatus = MeldekortBehandlingStatus.GODKJENT,
        brukersMeldekort: BrukersMeldekort = brukersMeldekort(
            sakId = sakId,
            meldeperiode = meldeperiode,
            behandlesAutomatisk = true,
            mottatt = nå(clock),
        ),
        simulering: Simulering? = null,
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
            status = status,
            simulering = simulering,
        )
    }

    /**
     * @param startDato Må starte på en mandag.
     */
    fun meldekortBeregning(
        sakId: SakId = SakId.random(),
        startDato: LocalDate = LocalDate.of(2023, 1, 2),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(
            Periode(startDato, startDato.plusDays(13)),
        ),
        meldekortId: MeldekortId = MeldekortId.random(),
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        maksDagerMedTiltakspengerForPeriode: Int = MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
        beregningDager: NonEmptyList<MeldeperiodeBeregningDag> = maksAntallDeltattTiltaksdagerIMeldekortperiode(
            startDato,
            meldekortId,
            tiltakstype,
            barnetilleggsPerioder,
        ),
    ): MeldekortBeregning {
        return MeldekortBeregning(
            nonEmptyListOf(
                MeldeperiodeBeregning(
                    kjedeId = kjedeId,
                    beregningMeldekortId = meldekortId,
                    dagerMeldekortId = meldekortId,
                    dager = beregningDager,
                ),
            ),
        )
    }

    fun maksAntallDeltattTiltaksdagerIMeldekortperiode(
        startDato: LocalDate,
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett,
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
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
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
    ): NonEmptyList<MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket> {
        require(antallDager in 1..5) {
            "Antall sammenhengende dager vil aldri være mer mindre enn 1 eller mer enn 5, men var $antallDager"
        }
        return List(antallDager) { index ->
            val dato = startDato.plusDays(index.toLong())
            MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket.create(
                dato = dato,
                tiltakstype = tiltakstype,
                antallBarn = barnetilleggsPerioder.hentVerdiForDag(dato) ?: AntallBarn.ZERO,
            )
        }.toNonEmptyListOrNull()!!
    }

    fun tiltaksdagerMedLønn(
        startDato: LocalDate,
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        antallDager: Int = 5,
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
    ): NonEmptyList<MeldeperiodeBeregningDag.Deltatt.DeltattMedLønnITiltaket> {
        require(antallDager in 1..5) {
            "Antall sammenhengende dager vil aldri være mer mindre enn 1 eller mer enn 5, men var $antallDager"
        }
        return List(antallDager) { index ->
            val dato = startDato.plusDays(index.toLong())
            MeldeperiodeBeregningDag.Deltatt.DeltattMedLønnITiltaket.create(
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
    ): NonEmptyList<MeldeperiodeBeregningDag.IkkeDeltatt> {
        require(antallDager in 1..5) {
            "Antall sammenhengende dager vil aldri være mer mindre enn 1 eller mer enn 5, men var $antallDager"
        }
        return List(antallDager) { index ->
            val dato = startDato.plusDays(index.toLong())
            MeldeperiodeBeregningDag.IkkeDeltatt.create(
                dato = dato,
                tiltakstype = tiltakstype,
                antallBarn = barnetilleggsPerioder.hentVerdiForDag(dato) ?: AntallBarn.ZERO,
            )
        }.toNonEmptyListOrNull()!!
    }

    suspend fun beregnMeldekortperioder(
        clock: Clock = TikkendeKlokke(),
        vurderingsperiode: Periode,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        meldeperioder: NonEmptyList<NonEmptyList<Dager.Dag>>,
        utfallsperioder: Periodisering<Utfallsperiode> = Periodisering(
            initiellVerdi = Utfallsperiode.RETT_TIL_TILTAKSPENGER,
            totalPeriode = vurderingsperiode,
        ),
        navkontor: Navkontor = ObjectMother.navkontor(),
        barnetilleggsPerioder: Periodisering<AntallBarn?> = Periodisering.empty(),
        begrunnelse: MeldekortBehandlingBegrunnelse? = null,
    ): MeldekortBehandlinger {
        val kommandoer = meldeperioder.map { meldeperiode ->
            OppdaterMeldekortKommando(
                sakId = sakId,
                meldekortId = MeldekortId.random(),
                saksbehandler = saksbehandler,
                dager = Dager(meldeperiode),
                correlationId = CorrelationId.generate(),
                begrunnelse = begrunnelse,
            )
        }

        val opprettet = nå(clock)

        return kommandoer.drop(1).foldIndexed(
            førsteBeregnetMeldekort(
                clock = clock,
                vurderingsperiode = vurderingsperiode,
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
                vurderingsperiode = vurderingsperiode,
                barnetilleggsPerioder = barnetilleggsPerioder,
                beslutter = beslutter,
                opprettet = opprettet.plusMinutes(1 + index.toLong()),
            )
        }
    }

    suspend fun førsteBeregnetMeldekort(
        kommando: OppdaterMeldekortKommando,
        vurderingsperiode: Periode,
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?> = Periodisering(
            TiltakstypeSomGirRett.GRUPPE_AMO,
            vurderingsperiode,
        ),
        meldekortId: MeldekortId,
        sakId: SakId,
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        fnr: Fnr = Fnr.random(),
        clock: Clock = TikkendeKlokke(),
        opprettet: LocalDateTime = nå(clock),
        status: MeldekortBehandlingStatus = MeldekortBehandlingStatus.UNDER_BEHANDLING,
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(kommando.periode),
        navkontor: Navkontor = ObjectMother.navkontor(),
        barnetilleggsPerioder: Periodisering<AntallBarn?> = Periodisering.empty(),
        girRett: Map<LocalDate, Boolean> = kommando.dager.dager.map { it.dag to it.status.girRett() }.toMap(),
        antallDagerForPeriode: Int = girRett.count { it.value },
        begrunnelse: MeldekortBehandlingBegrunnelse? = null,
        attesteringer: Attesteringer = Attesteringer.empty(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        simulering: Simulering? = null,
    ): Pair<MeldekortBehandlinger, MeldekortBehandletManuelt> {
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
        val meldekortBehandlinger = MeldekortBehandlinger(
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
                ),
            ),
        )

        return meldekortBehandlinger.sendTilBeslutter(
            kommando = kommando.tilSendMeldekortTilBeslutterKommando(),
            beregn = {
                // TODO jah: Det føles unaturlig og ikke gå via sak her.
                beregn(
                    meldekortIdSomBeregnes = meldekortId,
                    meldeperiodeSomBeregnes = dager,
                    barnetilleggsPerioder = barnetilleggsPerioder,
                    tiltakstypePerioder = tiltakstypePerioder,
                    meldekortBehandlinger = meldekortBehandlinger,
                )
            },
            simuler = {
                simulering
                    ?.let { ObjectMother.simuleringMedMetadata(simulering = simulering).right() }
                    ?: KunneIkkeSimulere.UkjentFeil.left()
            },
            clock = clock,
        )
            .map { (meldekortBehandlinger, meldekort) ->
                val tildeltMeldekort = meldekort.taMeldekortBehandling(beslutter) as MeldekortBehandletManuelt
                val iverksattMeldekort = tildeltMeldekort.iverksettMeldekort(beslutter, clock).getOrFail()
                val oppdaterteBehandlinger = meldekortBehandlinger.oppdaterMeldekortbehandling(iverksattMeldekort)
                Pair(oppdaterteBehandlinger, iverksattMeldekort)
            }
            .getOrFail()
    }

    suspend fun MeldekortBehandlinger.beregnNesteMeldekort(
        kommando: OppdaterMeldekortKommando,
        vurderingsperiode: Periode,
        fnr: Fnr,
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(kommando.periode),
        navkontor: Navkontor = ObjectMother.navkontor(),
        clock: Clock = TikkendeKlokke(),
        opprettet: LocalDateTime = nå(clock),
        barnetilleggsPerioder: Periodisering<AntallBarn?>,
        status: MeldekortBehandlingStatus = MeldekortBehandlingStatus.UNDER_BEHANDLING,
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?> = Periodisering(
            TiltakstypeSomGirRett.GRUPPE_AMO,
            vurderingsperiode,
        ),
        girRett: Map<LocalDate, Boolean> = kommando.dager.dager.map { it.dag to it.status.girRett() }.toMap(),
        antallDagerForPeriode: Int = girRett.count { it.value },
        attesteringer: Attesteringer = Attesteringer.empty(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
    ): MeldekortBehandlinger {
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
        return this.leggTil(
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
            ),
        ).sendTilBeslutter(
            kommando = kommando.tilSendMeldekortTilBeslutterKommando(),
            beregn = {
                // TODO jah: Det føles unaturlig og ikke gå via sak her.
                beregn(
                    meldekortIdSomBeregnes = meldekortId,
                    meldeperiodeSomBeregnes = dager,
                    barnetilleggsPerioder = barnetilleggsPerioder,
                    tiltakstypePerioder = tiltakstypePerioder,
                    meldekortBehandlinger = this,
                )
            },
            simuler = { KunneIkkeSimulere.UkjentFeil.left() },
            clock,
        )
            .map { (meldekortBehandlinger, meldekort) ->
                val tildeltMeldekort = meldekort.taMeldekortBehandling(beslutter) as MeldekortBehandletManuelt
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
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        fnr: Fnr = Fnr.random(),
        opprettet: LocalDateTime = nå(clock),
        antallDagerForPeriode: Int = 10,
        girRett: Map<LocalDate, Boolean> = buildMap {
            val perUke = ceil(antallDagerForPeriode / 2.0).toInt()
            val helg = 7 - perUke
            (0 until perUke).forEach { day ->
                put(periode.fraOgMed.plusDays(day.toLong()), true)
            }
            (perUke until 7).forEach { day ->
                put(periode.fraOgMed.plusDays(day.toLong()), false)
            }
            (7 until 14).forEach { day ->
                put(periode.fraOgMed.plusDays(day.toLong()), true)
            }
            ((14 - helg) until 14).forEach { day ->
                put(periode.fraOgMed.plusDays(day.toLong()), false)
            }
        },
        rammevedtak: Periodisering<VedtakId?> = Periodisering(VedtakId.random(), periode),
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
        mottatt: LocalDateTime = LocalDateTime.now(),
        sakId: SakId = SakId.random(),
        meldeperiode: Meldeperiode = meldeperiode(
            sakId = sakId,
            // Meldeperioden kommer før innsendingen.
            opprettet = mottatt.minus(1, ChronoUnit.MILLIS),
        ),
        dager: List<BrukersMeldekortDag> = buildList {
            val dagerFraPeriode = meldeperiode.periode.tilDager()
            require(dagerFraPeriode.size == 14)
            addAll(dagerFraPeriode.take(5).map { BrukersMeldekortDag(InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET, it) })
            addAll(dagerFraPeriode.subList(5, 7).map { BrukersMeldekortDag(InnmeldtStatus.IKKE_BESVART, it) })
            addAll(
                dagerFraPeriode.subList(7, 12)
                    .map { BrukersMeldekortDag(InnmeldtStatus.DELTATT_UTEN_LØNN_I_TILTAKET, it) },
            )
            addAll(dagerFraPeriode.subList(12, 14).map { BrukersMeldekortDag(InnmeldtStatus.IKKE_BESVART, it) })
        },
        behandlesAutomatisk: Boolean = false,
        behandletAutomatiskStatus: BrukersMeldekortBehandletAutomatiskStatus? = null,
    ): BrukersMeldekort {
        return BrukersMeldekort(
            id = id,
            mottatt = mottatt,
            meldeperiode = meldeperiode,
            sakId = sakId,
            dager = dager,
            journalpostId = JournalpostIdGenerator().neste(),
            oppgaveId = null,
            behandlesAutomatisk = behandlesAutomatisk,
            behandletAutomatiskStatus = behandletAutomatiskStatus,
        )
    }

    fun lagreBrukersMeldekortKommando(
        id: MeldekortId = MeldekortId.random(),
        mottatt: LocalDateTime = LocalDateTime.now(),
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
            journalpostId = JournalpostIdGenerator().neste(),
        )
    }

    fun oppdaterMeldekortKommando(
        sakId: SakId = SakId.random(),
        meldekortId: MeldekortId = MeldekortId.random(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        begrunnelse: MeldekortBehandlingBegrunnelse? = null,
        correlationId: CorrelationId = CorrelationId.generate(),
        dager: Dager,
    ): OppdaterMeldekortKommando {
        return OppdaterMeldekortKommando(
            sakId = sakId,
            meldekortId = meldekortId,
            saksbehandler = saksbehandler,
            dager = dager,
            begrunnelse = begrunnelse,
            correlationId = correlationId,
        )
    }

    fun sendMeldekortTilBeslutterKommando(
        sakId: SakId = SakId.random(),
        meldekortId: MeldekortId = MeldekortId.random(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        begrunnelse: MeldekortBehandlingBegrunnelse? = null,
        correlationId: CorrelationId = CorrelationId.generate(),
        dager: Dager,
    ): SendMeldekortTilBeslutterKommando {
        return SendMeldekortTilBeslutterKommando(
            sakId = sakId,
            meldekortId = meldekortId,
            saksbehandler = saksbehandler,
            dager = dager,
            begrunnelse = begrunnelse,
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
                MeldekortDagStatus.SPERRET -> OppdaterMeldekortKommando.Status.SPERRET
                MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> OppdaterMeldekortKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
                MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> OppdaterMeldekortKommando.Status.DELTATT_MED_LØNN_I_TILTAKET
                MeldekortDagStatus.IKKE_DELTATT -> OppdaterMeldekortKommando.Status.IKKE_DELTATT
                MeldekortDagStatus.FRAVÆR_SYK -> OppdaterMeldekortKommando.Status.FRAVÆR_SYK
                MeldekortDagStatus.FRAVÆR_SYKT_BARN -> OppdaterMeldekortKommando.Status.FRAVÆR_SYKT_BARN
                MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> OppdaterMeldekortKommando.Status.FRAVÆR_GODKJENT_AV_NAV
                MeldekortDagStatus.FRAVÆR_ANNET -> OppdaterMeldekortKommando.Status.FRAVÆR_ANNET
                MeldekortDagStatus.IKKE_BESVART -> if (dag.dato.erHelg()) {
                    OppdaterMeldekortKommando.Status.IKKE_DELTATT
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
                MeldekortDagStatus.SPERRET -> OppdaterMeldekortKommando.Status.SPERRET
                MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> OppdaterMeldekortKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
                MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> OppdaterMeldekortKommando.Status.DELTATT_MED_LØNN_I_TILTAKET
                MeldekortDagStatus.IKKE_DELTATT -> OppdaterMeldekortKommando.Status.IKKE_DELTATT
                MeldekortDagStatus.FRAVÆR_SYK -> OppdaterMeldekortKommando.Status.FRAVÆR_SYK
                MeldekortDagStatus.FRAVÆR_SYKT_BARN -> OppdaterMeldekortKommando.Status.FRAVÆR_SYKT_BARN
                MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> OppdaterMeldekortKommando.Status.FRAVÆR_GODKJENT_AV_NAV
                MeldekortDagStatus.FRAVÆR_ANNET -> OppdaterMeldekortKommando.Status.FRAVÆR_ANNET
                MeldekortDagStatus.IKKE_BESVART -> if (dag.dato.erHelg()) {
                    OppdaterMeldekortKommando.Status.IKKE_DELTATT
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
 * @throws IllegalStateException Dersom alle dagene i en meldekortperiode er SPERRET er den per definisjon utfylt. Dette har vi ikke støtte for i MVP.
 */
private fun genererMeldekortdagerFraMeldeperiode(
    meldeperiode: Meldeperiode,
): MeldekortDager {
    val dager = meldeperiode.girRett.entries.map { (dato, girRett) ->
        MeldekortDag(
            dato = dato,
            status = if (girRett) MeldekortDagStatus.IKKE_BESVART else MeldekortDagStatus.SPERRET,
        )
    }

    return if (dager.any { it.status == MeldekortDagStatus.IKKE_BESVART }) {
        MeldekortDager(dager, meldeperiode)
    } else {
        throw IllegalStateException("Alle dagene i en meldekortperiode er SPERRET. Dette har vi ikke støtte for i MVP.")
    }
}

fun OppdaterMeldekortKommando.tilSendMeldekortTilBeslutterKommando(): SendMeldekortTilBeslutterKommando {
    return SendMeldekortTilBeslutterKommando(
        sakId = sakId,
        meldekortId = meldekortId,
        saksbehandler = saksbehandler,
        dager = dager,
        begrunnelse = begrunnelse,
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
            addAll(dagerFraPeriode.subList(5, 7).map { Dager.Dag(it, OppdaterMeldekortKommando.Status.IKKE_DELTATT) })
            addAll(
                dagerFraPeriode.subList(7, 12)
                    .map { Dager.Dag(it, OppdaterMeldekortKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET) },
            )
            addAll(dagerFraPeriode.subList(12, 14).map { Dager.Dag(it, OppdaterMeldekortKommando.Status.IKKE_DELTATT) })
        }.toNonEmptyListOrNull()!!,
    )
}
