package no.nav.tiltakspenger.saksbehandling.objectmothers

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.januar
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.common.JournalpostIdGenerator
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Utfallsperiode
import no.nav.tiltakspenger.saksbehandling.felles.erHelg
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort.BrukersMeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.LagreBrukersMeldekortKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingType
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBeregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortbehandlingBegrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Dager
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.ceil

interface MeldekortMother : MotherOfAllMothers {
    @Suppress("unused")
    fun meldekortUnderBehandling(
        id: MeldekortId = MeldekortId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        fnr: Fnr = Fnr.random(),
        periode: Periode = Periode(6.januar(2025), 19.januar(2025)),
        meldekortperiode: MeldekortBeregning.IkkeUtfyltMeldeperiode = ikkeUtfyltMeldekortperiode(
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
    ): MeldekortBehandling.MeldekortUnderBehandling {
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
            type = type,
            attesteringer = attesteringer,
            begrunnelse = null,
            sendtTilBeslutning = null,
        )
    }

    fun meldekortBehandlet(
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
        meldekortperiodeBeregning: MeldekortBeregning.UtfyltMeldeperiode =
            utfyltMeldekortperiode(
                meldekortId = id,
                sakId = sakId,
                startDato = meldeperiode.periode.fraOgMed,
                barnetilleggsPerioder = barnetilleggsPerioder,
                opprettet = opprettet,
            ),

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
        begrunnelse: MeldekortbehandlingBegrunnelse? = null,
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
            type = type,
            begrunnelse = begrunnelse,
            attesteringer = attesteringer,
        )
    }

    /**
     * @param startDato Må starte på en mandag.
     */
    fun utfyltMeldekortperiode(
        sakId: SakId = SakId.random(),
        startDato: LocalDate = LocalDate.of(2023, 1, 2),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(
            Periode(startDato, startDato.plusDays(13)),
        ),
        meldekortId: MeldekortId = MeldekortId.random(),
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        maksDagerMedTiltakspengerForPeriode: Int = Behandling.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
        barnetilleggsPerioder: Periodisering<AntallBarn> = Periodisering.empty(),
        dager: NonEmptyList<MeldeperiodeBeregningDag.Utfylt> = maksAntallDeltattTiltaksdagerIMeldekortperiode(
            startDato,
            meldekortId,
            tiltakstype,
            barnetilleggsPerioder,
        ),
        opprettet: LocalDateTime = LocalDateTime.now(clock),
        beregninger: NonEmptyList<MeldekortBeregning.MeldeperiodeBeregnet> = nonEmptyListOf(
            MeldekortBeregning.MeldeperiodeBeregnet(
                kjedeId = kjedeId,
                meldekortId = meldekortId,
                dager = dager,
                opprettet = opprettet,
            ),
        ),
    ): MeldekortBeregning.UtfyltMeldeperiode {
        return MeldekortBeregning.UtfyltMeldeperiode(
            sakId = sakId,
            maksDagerMedTiltakspengerForPeriode = maksDagerMedTiltakspengerForPeriode,
            beregninger = beregninger,
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
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?> = Periodisering(
            TiltakstypeSomGirRett.GRUPPE_AMO,
            periode,
        ),
        maksDagerMedTiltakspengerForPeriode: Int = Behandling.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE,
    ): MeldekortBeregning.IkkeUtfyltMeldeperiode {
        val meldeperiode = meldeperiode(
            periode = periode,
            sakId = sakId,
            antallDagerForPeriode = maksDagerMedTiltakspengerForPeriode,
        )

        return MeldekortBeregning.IkkeUtfyltMeldeperiode.fraPeriode(
            meldeperiode = meldeperiode,
            meldekortId = meldekortId,
            sakId = sakId,
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
        startDato: LocalDate,
        meldekortId: MeldekortId,
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
        beslutter: Saksbehandler = ObjectMother.beslutter(),
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        meldeperioder: NonEmptyList<NonEmptyList<Dager.Dag>>,
        utfallsperioder: Periodisering<Utfallsperiode> = Periodisering(
            initiellVerdi = Utfallsperiode.RETT_TIL_TILTAKSPENGER,
            totalePeriode = vurderingsperiode,
        ),
        navkontor: Navkontor = ObjectMother.navkontor(),
        barnetilleggsPerioder: Periodisering<AntallBarn?> = Periodisering.empty(),
        begrunnelse: MeldekortbehandlingBegrunnelse? = null,
    ): MeldekortBehandlinger {
        val kommandoer = meldeperioder.map { meldeperiode ->
            SendMeldekortTilBeslutningKommando(
                sakId = sakId,
                meldekortId = MeldekortId.random(),
                saksbehandler = saksbehandler,
                dager = Dager(meldeperiode),
                correlationId = CorrelationId.generate(),
                meldekortbehandlingBegrunnelse = begrunnelse,
            )
        }

        val opprettet = nå(clock)

        return kommandoer.drop(1).foldIndexed(
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
                beslutter = beslutter,
                opprettet = opprettet,
            ).first,
        ) { index, meldekortperioder, kommando ->
            meldekortperioder.beregnNesteMeldekort(
                kommando,
                fnr,
                barnetilleggsPerioder = barnetilleggsPerioder,
                beslutter = beslutter,
                opprettet = opprettet.plusMinutes(1 + index.toLong()),
            )
        }
    }

    fun førsteBeregnetMeldekort(
        kommando: SendMeldekortTilBeslutningKommando,
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?> = Periodisering(
            TiltakstypeSomGirRett.GRUPPE_AMO,
            kommando.periode,
        ),
        meldekortId: MeldekortId,
        sakId: SakId,
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        fnr: Fnr = Fnr.random(),
        clock: Clock = fixedClock,
        opprettet: LocalDateTime = nå(clock),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(kommando.periode),
        navkontor: Navkontor = ObjectMother.navkontor(),
        barnetilleggsPerioder: Periodisering<AntallBarn?> = Periodisering.empty(),
        girRett: Map<LocalDate, Boolean> = kommando.dager.dager.map { it.dag to it.status.girRett() }.toMap(),
        antallDagerForPeriode: Int = girRett.count { it.value },
        begrunnelse: MeldekortbehandlingBegrunnelse? = null,
        attesteringer: Attesteringer = Attesteringer.empty(),
        beslutter: Saksbehandler = ObjectMother.beslutter(),
    ): Pair<MeldekortBehandlinger, MeldekortBehandling.MeldekortBehandlet> {
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

        val meldekortBehandlinger = MeldekortBehandlinger(
            verdi = nonEmptyListOf(
                MeldekortBehandling.MeldekortUnderBehandling(
                    id = meldekortId,
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    opprettet = opprettet,
                    navkontor = navkontor,
                    beregning = MeldekortBeregning.IkkeUtfyltMeldeperiode.fraPeriode(
                        sakId = sakId,
                        meldeperiode = meldeperiode,
                        tiltakstypePerioder = tiltakstypePerioder,
                        meldekortId = meldekortId,
                    ),
                    ikkeRettTilTiltakspengerTidspunkt = null,
                    meldeperiode = meldeperiode,
                    brukersMeldekort = null,
                    saksbehandler = kommando.saksbehandler.navIdent,
                    type = MeldekortBehandlingType.FØRSTE_BEHANDLING,
                    begrunnelse = begrunnelse,
                    attesteringer = attesteringer,
                    sendtTilBeslutning = null,
                ),
            ),
        )
        return meldekortBehandlinger
            .sendTilBeslutter(kommando, barnetilleggsPerioder, tiltakstypePerioder, clock)
            .map { (meldekortBehandlinger, meldekort) ->
                val iverksattMeldekort = meldekort.iverksettMeldekort(beslutter, clock).getOrFail()
                val oppdaterteBehandlinger = meldekortBehandlinger.oppdaterMeldekortbehandling(iverksattMeldekort)
                Pair(oppdaterteBehandlinger, iverksattMeldekort)
            }
            .getOrFail()
    }

    fun MeldekortBehandlinger.beregnNesteMeldekort(
        kommando: SendMeldekortTilBeslutningKommando,
        fnr: Fnr,
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(kommando.periode),
        navkontor: Navkontor = ObjectMother.navkontor(),
        clock: Clock = fixedClock,
        opprettet: LocalDateTime = nå(clock),
        barnetilleggsPerioder: Periodisering<AntallBarn?>,
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?> = Periodisering(
            TiltakstypeSomGirRett.GRUPPE_AMO,
            kommando.periode,
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

        return this.leggTil(
            MeldekortBehandling.MeldekortUnderBehandling(
                id = meldekortId,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                opprettet = opprettet,
                navkontor = navkontor,
                beregning = MeldekortBeregning.IkkeUtfyltMeldeperiode.fraPeriode(
                    sakId = sakId,
                    meldeperiode = meldeperiode,
                    tiltakstypePerioder = tiltakstypePerioder,
                    meldekortId = meldekortId,
                ),
                ikkeRettTilTiltakspengerTidspunkt = null,
                meldeperiode = meldeperiode,
                brukersMeldekort = null,
                saksbehandler = kommando.saksbehandler.navIdent,
                type = MeldekortBehandlingType.FØRSTE_BEHANDLING,
                begrunnelse = null,
                attesteringer = attesteringer,
                sendtTilBeslutning = null,
            ),
        ).sendTilBeslutter(kommando, barnetilleggsPerioder, tiltakstypePerioder, clock)
            .map { (meldekortBehandlinger, meldekort) ->
                val iverksattMeldekort = meldekort.iverksettMeldekort(beslutter, clock).getOrFail()
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

    fun lagreBrukersMeldekortKommando(
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
    ): LagreBrukersMeldekortKommando {
        return LagreBrukersMeldekortKommando(
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
        meldekortbehandlingBegrunnelse = null,
    )
}
