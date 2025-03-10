package no.nav.tiltakspenger.objectmothers

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.common.JournalpostIdGenerator
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.vedtak.barnetillegg.AntallBarn
import no.nav.tiltakspenger.vedtak.felles.Navkontor
import no.nav.tiltakspenger.vedtak.felles.erHelg
import no.nav.tiltakspenger.vedtak.felles.nå
import no.nav.tiltakspenger.vedtak.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.vedtak.meldekort.domene.BrukersMeldekort.BrukersMeldekortDag
import no.nav.tiltakspenger.vedtak.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.vedtak.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.vedtak.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.vedtak.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.vedtak.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.vedtak.meldekort.domene.MeldeperiodeBeregning
import no.nav.tiltakspenger.vedtak.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.vedtak.meldekort.domene.NyttBrukersMeldekort
import no.nav.tiltakspenger.vedtak.meldekort.domene.SendMeldekortTilBeslutningKommando
import no.nav.tiltakspenger.vedtak.meldekort.domene.SendMeldekortTilBeslutningKommando.Dager
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.vilkår.Utfallsperiode
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.ceil

interface MeldekortMother {
    @Suppress("unused")
    fun meldekortUnderBehandling(
        id: MeldekortId = MeldekortId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        fnr: Fnr = Fnr.random(),
        periode: Periode,
        meldekortperiode: MeldeperiodeBeregning.IkkeUtfyltMeldeperiode = ikkeUtfyltMeldekortperiode(
            meldekortId = id,
            sakId = sakId,
            periode = periode,
        ),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(meldekortperiode.periode),
        saksbehandler: String = "saksbehandler",
        beslutter: String = "beslutter",
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        status: MeldekortBehandlingStatus = MeldekortBehandlingStatus.GODKJENT,
        navkontor: Navkontor = ObjectMother.navkontor(),
        opprettet: LocalDateTime = nå(),
    ): MeldekortBehandling.MeldekortUnderBehandling {
        val meldeperiode = meldeperiode(
            periode = periode,
            kjedeId = kjedeId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
        )

        return MeldekortBehandling.MeldekortUnderBehandling(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            beregning = meldekortperiode,
            navkontor = navkontor,
            ikkeRettTilTiltakspengerTidspunkt = null,
            meldeperiode = meldeperiode,
            brukersMeldekort = null,
            saksbehandler = saksbehandler,
        )
    }

    fun meldekortBehandlet(
        id: MeldekortId = MeldekortId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        fnr: Fnr = Fnr.random(),
        periode: Periode,
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
        opprettet: LocalDateTime = nå(),
        meldeperiode: Meldeperiode = meldeperiode(
            periode = periode,
            kjedeId = kjedeId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
        ),
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
        meldekortperiodeBeregning: MeldeperiodeBeregning.UtfyltMeldeperiode =
            utfyltMeldekortperiode(
                meldekortId = id,
                sakId = sakId,
                startDato = meldeperiode.periode.fraOgMed,
                barnetilleggsPerioder = barnetilleggsPerioder,
            ),

        saksbehandler: String = "saksbehandler",
        beslutter: String = "beslutter",
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        status: MeldekortBehandlingStatus = MeldekortBehandlingStatus.GODKJENT,
        iverksattTidspunkt: LocalDateTime? = nå(),
        navkontor: Navkontor = ObjectMother.navkontor(),
        antallDagerForMeldeperiode: Int = 14,
        sendtTilBeslutning: LocalDateTime = nå(),
    ): MeldekortBehandling.MeldekortBehandlet {
        return MeldekortBehandling.MeldekortBehandlet(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            beregning = meldekortperiodeBeregning,
            saksbehandler = saksbehandler,
            sendtTilBeslutning = sendtTilBeslutning,
            beslutter = beslutter,
            status = status,
            iverksattTidspunkt = iverksattTidspunkt,
            navkontor = navkontor,
            ikkeRettTilTiltakspengerTidspunkt = null,
            meldeperiode = meldeperiode,
            brukersMeldekort = null,
        )
    }

    /**
     * @param startDato Må starte på en mandag.
     */
    fun utfyltMeldekortperiode(
        sakId: SakId = SakId.random(),
        startDato: LocalDate = LocalDate.of(2023, 1, 2),
        meldekortId: MeldekortId = MeldekortId.random(),
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        maksDagerMedTiltakspengerForPeriode: Int = 14,
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
    ): MeldeperiodeBeregning.UtfyltMeldeperiode {
        return MeldeperiodeBeregning.UtfyltMeldeperiode(
            sakId = sakId,
            maksDagerMedTiltakspengerForPeriode = maksDagerMedTiltakspengerForPeriode,
            dager = maksAntallDeltattTiltaksdagerIMeldekortperiode(
                startDato,
                meldekortId,
                tiltakstype,
                barnetilleggsPerioder,
            ),
        )
    }

    /**
     * @param startDato Må starte på en mandag.
     */
    fun ikkeUtfyltMeldekortperiode(
        sakId: SakId = SakId.random(),
        periode: Periode,
        startDato: LocalDate = LocalDate.of(2023, 1, 2),
        meldekortId: MeldekortId = MeldekortId.random(),
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett> = Periodisering(
            TiltakstypeSomGirRett.GRUPPE_AMO,
            periode,
        ),
        maksDagerMedTiltakspengerForPeriode: Int = 14,
    ): MeldeperiodeBeregning.IkkeUtfyltMeldeperiode {
        val meldeperiode = meldeperiode(
            periode = periode,
            sakId = sakId,
        )

        return MeldeperiodeBeregning.IkkeUtfyltMeldeperiode.fraPeriode(
            meldeperiode = meldeperiode,
            meldekortId = meldekortId,
            sakId = sakId,
            maksDagerMedTiltakspengerForPeriode = maksDagerMedTiltakspengerForPeriode,

            tiltakstypePerioder = tiltakstypePerioder,

        )
    }

    fun maksAntallDeltattTiltaksdagerIMeldekortperiode(
        startDato: LocalDate,
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett,
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
    ): NonEmptyList<MeldeperiodeBeregningDag.Utfylt> {
        return (
            tiltaksdager(startDato, meldekortId, tiltakstype, barnetilleggsPerioder = barnetilleggsPerioder) +
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
        startDato: LocalDate = LocalDate.of(2023, 1, 2),
        meldekortId: MeldekortId = MeldekortId.random(),
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        antallDager: Int = 5,
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
    ): NonEmptyList<MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattUtenLønnITiltaket> {
        require(antallDager in 1..5) {
            "Antall sammenhengende dager vil aldri være mer mindre enn 1 eller mer enn 5, men var $antallDager"
        }
        return List(antallDager) { index ->
            val dato = startDato.plusDays(index.toLong())
            MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattUtenLønnITiltaket.create(
                dato = dato,
                meldekortId = meldekortId,
                tiltakstype = tiltakstype,
                antallBarn = barnetilleggsPerioder.hentVerdiForDag(dato) ?: AntallBarn.ZERO,
            )
        }.toNonEmptyListOrNull()!!
    }

    fun ikkeTiltaksdager(
        startDato: LocalDate = LocalDate.of(2023, 1, 2),
        meldekortId: MeldekortId = MeldekortId.random(),
        antallDager: Int = 2,
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
    ): NonEmptyList<MeldeperiodeBeregningDag.Utfylt.IkkeDeltatt> {
        require(antallDager in 1..5) {
            "Antall sammenhengende dager vil aldri være mer mindre enn 1 eller mer enn 5, men var $antallDager"
        }
        return List(antallDager) { index ->
            val dato = startDato.plusDays(index.toLong())
            MeldeperiodeBeregningDag.Utfylt.IkkeDeltatt.create(
                dato = dato,
                meldekortId = meldekortId,
                tiltakstype = tiltakstype,
                antallBarn = barnetilleggsPerioder.hentVerdiForDag(dato) ?: AntallBarn.ZERO,
            )
        }.toNonEmptyListOrNull()!!
    }

    fun beregnMeldekortperioder(
        vurderingsperiode: Periode,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        meldeperioder: NonEmptyList<NonEmptyList<Dager.Dag>>,
        utfallsperioder: Periodisering<Utfallsperiode> = Periodisering(
            initiellVerdi = Utfallsperiode.RETT_TIL_TILTAKSPENGER,
            totalePeriode = vurderingsperiode,
        ),
        navkontor: Navkontor = ObjectMother.navkontor(),
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
    ): MeldekortBehandlinger {
        val kommandoer = meldeperioder.map { meldeperiode ->
            SendMeldekortTilBeslutningKommando(
                sakId = sakId,
                meldekortId = MeldekortId.random(),
                saksbehandler = saksbehandler,
                dager = Dager(meldeperiode),
                correlationId = CorrelationId.generate(),
            )
        }

        return kommandoer.drop(1).fold(
            førsteBeregnetMeldekort(
                tiltakstypePerioder = Periodisering(
                    TiltakstypeSomGirRett.GRUPPE_AMO,
                    kommandoer.first().periode,
                ),
                meldekortId = kommandoer.first().meldekortId,
                sakId = sakId,
                fnr = fnr,
                kommando = kommandoer.first(),
                kjedeId = MeldeperiodeKjedeId.fraPeriode(kommandoer.first().periode),
            ).first,
        ) { meldekortperioder, kommando ->
            meldekortperioder.beregnNesteMeldekort(kommando, fnr, barnetilleggsPerioder = barnetilleggsPerioder)
        }
    }

    fun førsteBeregnetMeldekort(
        kommando: SendMeldekortTilBeslutningKommando,
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett> = Periodisering(
            TiltakstypeSomGirRett.GRUPPE_AMO,
            kommando.periode,
        ),
        meldekortId: MeldekortId,
        sakId: SakId,
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        fnr: Fnr = Fnr.random(),
        opprettet: LocalDateTime = nå(),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(kommando.periode),
        navkontor: Navkontor = ObjectMother.navkontor(),
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
    ): Pair<MeldekortBehandlinger, MeldekortBehandling.MeldekortBehandlet> {
        val meldeperiode = meldeperiode(
            periode = kommando.periode,
            kjedeId = kjedeId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            girRett = kommando.dager.dager.map { it.dag to it.status.girRett() }.toMap(),
        )

        val meldekortBehandlinger = MeldekortBehandlinger(
            verdi = nonEmptyListOf(
                MeldekortBehandling.MeldekortUnderBehandling(
                    id = meldekortId,
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    opprettet = opprettet,
                    navkontor = navkontor,
                    beregning = MeldeperiodeBeregning.IkkeUtfyltMeldeperiode.fraPeriode(
                        sakId = sakId,
                        meldeperiode = meldeperiode,
                        tiltakstypePerioder = tiltakstypePerioder,
                        meldekortId = meldekortId,
                        maksDagerMedTiltakspengerForPeriode = kommando.dager.size,
                    ),
                    ikkeRettTilTiltakspengerTidspunkt = null,
                    meldeperiode = meldeperiode,
                    brukersMeldekort = null,
                    saksbehandler = kommando.saksbehandler.navIdent,
                ),
            ),
        )
        return meldekortBehandlinger.sendTilBeslutter(kommando, barnetilleggsPerioder, tiltakstypePerioder).getOrFail()
    }

    fun MeldekortBehandlinger.beregnNesteMeldekort(
        kommando: SendMeldekortTilBeslutningKommando,
        fnr: Fnr,
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(kommando.periode),
        navkontor: Navkontor = ObjectMother.navkontor(),
        opprettet: LocalDateTime = nå(),
        barnetilleggsPerioder: Periodisering<AntallBarn>,
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett> = Periodisering(
            TiltakstypeSomGirRett.GRUPPE_AMO,
            kommando.periode,
        ),
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
            girRett = kommando.dager.dager.map { it.dag to it.status.girRett() }.toMap(),
        )

        return MeldekortBehandlinger(
            verdi = this.verdi + MeldekortBehandling.MeldekortUnderBehandling(
                id = meldekortId,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                opprettet = opprettet,
                navkontor = navkontor,
                beregning = MeldeperiodeBeregning.IkkeUtfyltMeldeperiode.fraPeriode(
                    sakId = sakId,
                    meldeperiode = meldeperiode,
                    tiltakstypePerioder = tiltakstypePerioder,
                    meldekortId = meldekortId,
                    maksDagerMedTiltakspengerForPeriode = kommando.dager.size,
                ),
                ikkeRettTilTiltakspengerTidspunkt = null,
                meldeperiode = meldeperiode,
                brukersMeldekort = null,
                saksbehandler = kommando.saksbehandler.navIdent,
            ),
        ).sendTilBeslutter(kommando, barnetilleggsPerioder, tiltakstypePerioder).getOrFail().first
    }

    fun meldeperiode(
        id: MeldeperiodeId = MeldeperiodeId.random(),
        periode: Periode = ObjectMother.virkningsperiode(),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
        sakId: SakId = SakId.random(),
        versjon: HendelseVersjon = HendelseVersjon.ny(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        fnr: Fnr = Fnr.random(),
        opprettet: LocalDateTime = nå(),
        antallDagerForPeriode: Int = 14,
        girRett: Map<LocalDate, Boolean> = buildMap {
            val perUke = ceil(antallDagerForPeriode / 2.0).toInt()
            (0 until perUke).forEach { day ->
                put(periode.fraOgMed.plusDays(day.toLong()), true)
            }
            (perUke until 7).forEach { day ->
                put(periode.fraOgMed.plusDays(day.toLong()), false)
            }
            (8 until antallDagerForPeriode).forEach { day ->
                put(periode.fraOgMed.plusDays(day.toLong()), true)
            }
            (antallDagerForPeriode until 14).forEach { day ->
                put(periode.fraOgMed.plusDays(day.toLong()), false)
            }
        },
    ): Meldeperiode = Meldeperiode(
        kjedeId = kjedeId,
        id = id,
        versjon = versjon,
        sakId = sakId,
        saksnummer = saksnummer,
        fnr = fnr,
        opprettet = opprettet,
        periode = periode,
        antallDagerForPeriode = antallDagerForPeriode,
        girRett = girRett,
        sendtTilMeldekortApi = null,
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
            opprettet = mottatt.minus(1, java.time.temporal.ChronoUnit.MILLIS),
        ),
        dager: List<BrukersMeldekortDag> = buildList {
            val dagerFraPeriode = meldeperiode.periode.tilDager()
            require(dagerFraPeriode.size == 14)
            addAll(dagerFraPeriode.take(5).map { BrukersMeldekortDag(InnmeldtStatus.DELTATT, it) })
            addAll(dagerFraPeriode.subList(5, 7).map { BrukersMeldekortDag(InnmeldtStatus.IKKE_REGISTRERT, it) })
            addAll(dagerFraPeriode.subList(7, 12).map { BrukersMeldekortDag(InnmeldtStatus.DELTATT, it) })
            addAll(dagerFraPeriode.subList(12, 14).map { BrukersMeldekortDag(InnmeldtStatus.IKKE_REGISTRERT, it) })
        },
    ): BrukersMeldekort {
        return BrukersMeldekort(
            id = id,
            mottatt = mottatt,
            meldeperiode = meldeperiode,
            sakId = sakId,
            dager = dager,
            journalpostId = JournalpostIdGenerator().neste(),
            oppgaveId = null,
        )
    }

    fun nyttBrukersMeldekort(
        id: MeldekortId = MeldekortId.random(),
        mottatt: LocalDateTime = LocalDateTime.now(),
        sakId: SakId = SakId.random(),
        meldeperiodeId: MeldeperiodeId = MeldeperiodeId.random(),
        periode: Periode,
        dager: List<BrukersMeldekortDag> = buildList {
            val dagerFraPeriode = periode.tilDager()
            require(dagerFraPeriode.size == 14)
            addAll(dagerFraPeriode.take(5).map { BrukersMeldekortDag(InnmeldtStatus.DELTATT, it) })
            addAll(dagerFraPeriode.subList(5, 7).map { BrukersMeldekortDag(InnmeldtStatus.IKKE_REGISTRERT, it) })
            addAll(dagerFraPeriode.subList(7, 12).map { BrukersMeldekortDag(InnmeldtStatus.DELTATT, it) })
            addAll(dagerFraPeriode.subList(12, 14).map { BrukersMeldekortDag(InnmeldtStatus.IKKE_REGISTRERT, it) })
        },
    ): NyttBrukersMeldekort {
        return NyttBrukersMeldekort(
            id = id,
            mottatt = mottatt,
            meldeperiodeId = meldeperiodeId,
            sakId = sakId,
            dager = dager,
            journalpostId = JournalpostIdGenerator().neste(),
            oppgaveId = null,
        )
    }
}

fun MeldekortBehandling.MeldekortUnderBehandling.tilSendMeldekortTilBeslutterKommando(
    saksbehandler: Saksbehandler,
): SendMeldekortTilBeslutningKommando {
    val dager = beregning.map { dag ->
        Dager.Dag(
            dag = dag.dato,
            status = when (dag) {
                is MeldeperiodeBeregningDag.IkkeUtfylt -> if (dag.dato.erHelg()) {
                    SendMeldekortTilBeslutningKommando.Status.IKKE_DELTATT
                } else {
                    SendMeldekortTilBeslutningKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
                }

                is MeldeperiodeBeregningDag.Utfylt -> when (dag) {
                    is MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattMedLønnITiltaket -> SendMeldekortTilBeslutningKommando.Status.DELTATT_MED_LØNN_I_TILTAKET
                    is MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattUtenLønnITiltaket -> SendMeldekortTilBeslutningKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
                    is MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SykBruker -> SendMeldekortTilBeslutningKommando.Status.FRAVÆR_SYK
                    is MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SyktBarn -> SendMeldekortTilBeslutningKommando.Status.FRAVÆR_SYKT_BARN
                    is MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdGodkjentAvNav -> SendMeldekortTilBeslutningKommando.Status.FRAVÆR_VELFERD_GODKJENT_AV_NAV
                    is MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdIkkeGodkjentAvNav -> SendMeldekortTilBeslutningKommando.Status.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
                    is MeldeperiodeBeregningDag.Utfylt.IkkeDeltatt -> SendMeldekortTilBeslutningKommando.Status.IKKE_DELTATT
                    is MeldeperiodeBeregningDag.Utfylt.Sperret -> SendMeldekortTilBeslutningKommando.Status.SPERRET
                }
            },
        )
    }.toNonEmptyListOrNull()!!
    return SendMeldekortTilBeslutningKommando(
        sakId = sakId,
        meldekortId = id,
        saksbehandler = saksbehandler,
        dager = Dager(dager),
        correlationId = CorrelationId.generate(),
    )
}
