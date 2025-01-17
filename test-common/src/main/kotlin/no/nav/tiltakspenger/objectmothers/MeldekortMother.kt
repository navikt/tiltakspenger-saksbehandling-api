package no.nav.tiltakspenger.objectmothers

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.felles.erHelg
import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort.BrukersMeldekortDag
import no.nav.tiltakspenger.meldekort.domene.InnmeldtStatus
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlingStatus
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregning
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Dager
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.AvklartUtfallForPeriode
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.ceil

interface MeldekortMother {

    fun ikkeUtfyltMeldekort(
        id: MeldekortId = MeldekortId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        fnr: Fnr = Fnr.random(),
        rammevedtakId: VedtakId = VedtakId.random(),
        periode: Periode,
        meldekortperiode: MeldeperiodeBeregning.IkkeUtfyltMeldeperiode = ikkeUtfyltMeldekortperiode(
            meldekortId = id,
            sakId = sakId,
            periode = periode,
        ),
        meldeperiodeId: MeldeperiodeId = MeldeperiodeId.fraPeriode(meldekortperiode.periode),
        saksbehandler: String = "saksbehandler",
        beslutter: String = "beslutter",
        forrigeMeldekortId: MeldekortId? = null,
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        status: MeldekortBehandlingStatus = MeldekortBehandlingStatus.GODKJENT,
        navkontor: Navkontor? = null,
        opprettet: LocalDateTime = nå(),
    ): MeldekortBehandling.IkkeUtfyltMeldekort {
        val meldeperiode = meldeperiode(
            periode = periode,
            id = meldeperiodeId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
        )

        return MeldekortBehandling.IkkeUtfyltMeldekort(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            rammevedtakId = rammevedtakId,
            opprettet = opprettet,
            beregning = meldekortperiode,
            forrigeMeldekortId = forrigeMeldekortId,
            tiltakstype = tiltakstype,
            navkontor = navkontor,
            ikkeRettTilTiltakspengerTidspunkt = null,
            meldeperiode = meldeperiode,
            brukersMeldekort = null,
        )
    }

    fun utfyltMeldekort(
        id: MeldekortId = MeldekortId.random(),
        sakId: SakId = SakId.random(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        fnr: Fnr = Fnr.random(),
        rammevedtakId: VedtakId = VedtakId.random(),
        meldekortperiodeBeregning: MeldeperiodeBeregning.UtfyltMeldeperiode =
            utfyltMeldekortperiode(
                meldekortId = id,
                sakId = sakId,
            ),
        meldeperiodeId: MeldeperiodeId = MeldeperiodeId.fraPeriode(meldekortperiodeBeregning.periode),
        saksbehandler: String = "saksbehandler",
        beslutter: String = "beslutter",
        forrigeMeldekortId: MeldekortId? = null,
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        status: MeldekortBehandlingStatus = MeldekortBehandlingStatus.GODKJENT,
        iverksattTidspunkt: LocalDateTime? = nå(),
        navkontor: Navkontor = ObjectMother.navkontor(),
        antallDagerForMeldeperiode: Int = 10,
        opprettet: LocalDateTime = nå(),
        sendtTilBeslutning: LocalDateTime = nå(),
        meldeperiode: Meldeperiode = meldeperiode(
            periode = meldekortperiodeBeregning.periode,
            id = meldeperiodeId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
        ),

    ): MeldekortBehandling.UtfyltMeldekort {
        return MeldekortBehandling.UtfyltMeldekort(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            rammevedtakId = rammevedtakId,
            opprettet = opprettet,
            beregning = meldekortperiodeBeregning,
            saksbehandler = saksbehandler,
            sendtTilBeslutning = sendtTilBeslutning,
            beslutter = beslutter,
            forrigeMeldekortId = forrigeMeldekortId,
            tiltakstype = tiltakstype,
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
        maksDagerMedTiltakspengerForPeriode: Int = 10,
    ): MeldeperiodeBeregning.UtfyltMeldeperiode {
        return MeldeperiodeBeregning.UtfyltMeldeperiode(
            sakId = sakId,
            maksDagerMedTiltakspengerForPeriode = maksDagerMedTiltakspengerForPeriode,
            dager = maksAntallDeltattTiltaksdagerIMeldekortperiode(startDato, meldekortId, tiltakstype),
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
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        maksDagerMedTiltakspengerForPeriode: Int = 10,
        utfallsperioder: Periodisering<AvklartUtfallForPeriode> = Periodisering(
            initiellVerdi = AvklartUtfallForPeriode.OPPFYLT,
            totalePeriode = periode,
        ),
    ): MeldeperiodeBeregning.IkkeUtfyltMeldeperiode {
        val meldeperiode = meldeperiode(
            periode = periode,
            sakId = sakId,
        )

        return MeldeperiodeBeregning.IkkeUtfyltMeldeperiode.fraPeriode(
            sakId = sakId,
            maksDagerMedTiltakspengerForPeriode = maksDagerMedTiltakspengerForPeriode,
            meldeperiode = meldeperiode,
            utfallsperioder = utfallsperioder,
            tiltakstype = tiltakstype,
            meldekortId = meldekortId,

        )
    }

    fun maksAntallDeltattTiltaksdagerIMeldekortperiode(
        startDato: LocalDate,
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett,
    ): NonEmptyList<MeldeperiodeBeregningDag.Utfylt> {
        return (
            tiltaksdager(startDato, meldekortId, tiltakstype) +
                ikkeTiltaksdager(startDato.plusDays(5), meldekortId, 2, tiltakstype) +
                tiltaksdager(startDato.plusDays(7), meldekortId, tiltakstype) +
                ikkeTiltaksdager(startDato.plusDays(12), meldekortId, 2, tiltakstype)
            ).toNonEmptyListOrNull()!!
    }

    fun tiltaksdager(
        startDato: LocalDate = LocalDate.of(2023, 1, 2),
        meldekortId: MeldekortId = MeldekortId.random(),
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        antallDager: Int = 5,
    ): NonEmptyList<MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattUtenLønnITiltaket> {
        require(antallDager in 1..5) {
            "Antall sammenhengende dager vil aldri være mer mindre enn 1 eller mer enn 5, men var $antallDager"
        }
        return List(antallDager) { index ->
            MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattUtenLønnITiltaket.create(
                dato = startDato.plusDays(index.toLong()),
                meldekortId = meldekortId,
                tiltakstype = tiltakstype,
            )
        }.toNonEmptyListOrNull()!!
    }

    fun ikkeTiltaksdager(
        startDato: LocalDate = LocalDate.of(2023, 1, 2),
        meldekortId: MeldekortId = MeldekortId.random(),
        antallDager: Int = 2,
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
    ): NonEmptyList<MeldeperiodeBeregningDag.Utfylt.IkkeDeltatt> {
        require(antallDager in 1..5) {
            "Antall sammenhengende dager vil aldri være mer mindre enn 1 eller mer enn 5, men var $antallDager"
        }
        return List(antallDager) { index ->
            MeldeperiodeBeregningDag.Utfylt.IkkeDeltatt.create(
                dato = startDato.plusDays(index.toLong()),
                meldekortId = meldekortId,
                tiltakstype = tiltakstype,
            )
        }.toNonEmptyListOrNull()!!
    }

    fun beregnMeldekortperioder(
        vurderingsperiode: Periode,
        tiltakstype: TiltakstypeSomGirRett = TiltakstypeSomGirRett.GRUPPE_AMO,
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler(),
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.random(),
        meldeperioder: NonEmptyList<NonEmptyList<Dager.Dag>>,
        rammevedtakId: VedtakId = VedtakId.random(),
        utfallsperioder: Periodisering<AvklartUtfallForPeriode> = Periodisering(
            initiellVerdi = AvklartUtfallForPeriode.OPPFYLT,
            totalePeriode = vurderingsperiode,
        ),
        navkontor: Navkontor = ObjectMother.navkontor(),
    ): MeldekortBehandlinger {
        val kommandoer = meldeperioder.map { meldeperiode ->
            SendMeldekortTilBeslutterKommando(
                sakId = sakId,
                meldekortId = MeldekortId.random(),
                saksbehandler = saksbehandler,
                dager = Dager(meldeperiode),
                correlationId = CorrelationId.generate(),
            )
        }
        return kommandoer.drop(1).fold(
            førsteBeregnetMeldekort(
                tiltakstype = tiltakstype,
                meldekortId = kommandoer.first().meldekortId,
                sakId = sakId,
                fnr = fnr,
                rammevedtakId = rammevedtakId,
                kommando = kommandoer.first(),
                utfallsperioder = utfallsperioder,
                meldeperiodeId = MeldeperiodeId.fraPeriode(kommandoer.first().periode),
            ).first,
        ) { meldekortperioder, kommando ->
            meldekortperioder.beregnNesteMeldekort(vurderingsperiode, kommando, fnr)
        }
    }

    fun førsteBeregnetMeldekort(
        tiltakstype: TiltakstypeSomGirRett,
        meldekortId: MeldekortId,
        sakId: SakId,
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        fnr: Fnr = Fnr.random(),
        rammevedtakId: VedtakId,
        opprettet: LocalDateTime = nå(),
        kommando: SendMeldekortTilBeslutterKommando,
        meldeperiodeId: MeldeperiodeId = MeldeperiodeId.fraPeriode(kommando.periode),
        utfallsperioder: Periodisering<AvklartUtfallForPeriode>,
        navkontor: Navkontor = ObjectMother.navkontor(),
    ): Pair<MeldekortBehandlinger, MeldekortBehandling.UtfyltMeldekort> {
        val meldeperiode = meldeperiode(
            periode = kommando.periode,
            id = meldeperiodeId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
        )

        return MeldekortBehandlinger(
            tiltakstype = tiltakstype,
            verdi = nonEmptyListOf(
                MeldekortBehandling.IkkeUtfyltMeldekort(
                    id = meldekortId,
                    sakId = sakId,
                    saksnummer = saksnummer,
                    fnr = fnr,
                    rammevedtakId = rammevedtakId,
                    forrigeMeldekortId = null,
                    opprettet = opprettet,
                    tiltakstype = tiltakstype,
                    navkontor = navkontor,
                    beregning = MeldeperiodeBeregning.IkkeUtfyltMeldeperiode.fraPeriode(
                        sakId = sakId,
                        meldeperiode = meldeperiode,
                        utfallsperioder = utfallsperioder,
                        tiltakstype = tiltakstype,
                        meldekortId = meldekortId,
                        maksDagerMedTiltakspengerForPeriode = kommando.dager.size,
                    ),
                    ikkeRettTilTiltakspengerTidspunkt = null,
                    meldeperiode = meldeperiode,
                    brukersMeldekort = null,
                ),
            ),
        ).sendTilBeslutter(kommando).getOrFail()
    }

    fun MeldekortBehandlinger.beregnNesteMeldekort(
        vurderingsperiode: Periode,
        kommando: SendMeldekortTilBeslutterKommando,
        fnr: Fnr,
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        meldeperiodeId: MeldeperiodeId = MeldeperiodeId.fraPeriode(kommando.periode),
        navkontor: Navkontor = ObjectMother.navkontor(),
        opprettet: LocalDateTime = nå(),
    ): MeldekortBehandlinger {
        val meldekortId = kommando.meldekortId
        val sakId = kommando.sakId
        val rammevedtakId = VedtakId.random()
        val tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO
        val utfallsperioder = Periodisering(
            initiellVerdi = AvklartUtfallForPeriode.OPPFYLT,
            totalePeriode = vurderingsperiode,
        )
        val meldeperiode = meldeperiode(
            periode = kommando.periode,
            id = meldeperiodeId,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
        )

        return MeldekortBehandlinger(
            tiltakstype = tiltakstype,
            verdi = this.verdi + MeldekortBehandling.IkkeUtfyltMeldekort(
                id = meldekortId,
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                rammevedtakId = rammevedtakId,
                forrigeMeldekortId = this.verdi.last().id,
                opprettet = opprettet,
                tiltakstype = tiltakstype,
                navkontor = navkontor,
                beregning = MeldeperiodeBeregning.IkkeUtfyltMeldeperiode.fraPeriode(
                    sakId = sakId,
                    meldeperiode = meldeperiode,
                    utfallsperioder = utfallsperioder,
                    tiltakstype = tiltakstype,
                    meldekortId = meldekortId,
                    maksDagerMedTiltakspengerForPeriode = kommando.dager.size,
                ),
                ikkeRettTilTiltakspengerTidspunkt = null,
                meldeperiode = meldeperiode,
                brukersMeldekort = null,
            ),
        ).sendTilBeslutter(kommando).getOrFail().first
    }

    fun meldeperiode(
        periode: Periode = ObjectMother.vurderingsperiode(),
        id: MeldeperiodeId = MeldeperiodeId.fraPeriode(periode),
        hendelseId: HendelseId = HendelseId.random(),
        sakId: SakId = SakId.random(),
        versjon: HendelseVersjon = HendelseVersjon.ny(),
        saksnummer: Saksnummer = Saksnummer.genererSaknummer(løpenr = "1001"),
        fnr: Fnr = Fnr.random(),
        opprettet: LocalDateTime = nå(),
        antallDagerForPeriode: Int = 10,
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
        id = id,
        hendelseId = hendelseId,
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

    /**
     * @param meldeperiode Hvis denne sendes inn, bør [sakId] og [mottatt] også sendes inn.
     */
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
            val dager = meldeperiode.periode.tilDager()
            require(dager.size == 14)
            addAll(dager.take(5).map { BrukersMeldekortDag(InnmeldtStatus.DELTATT, it) })
            addAll(dager.subList(5, 7).map { BrukersMeldekortDag(InnmeldtStatus.IKKE_REGISTRERT, it) })
            addAll(dager.subList(7, 12).map { BrukersMeldekortDag(InnmeldtStatus.DELTATT, it) })
            addAll(dager.subList(12, 14).map { BrukersMeldekortDag(InnmeldtStatus.IKKE_REGISTRERT, it) })
        },
    ): BrukersMeldekort {
        return BrukersMeldekort(
            id = id,
            mottatt = mottatt,
            meldeperiode = meldeperiode,
            sakId = sakId,
            dager = dager,
        )
    }
}

fun MeldekortBehandling.IkkeUtfyltMeldekort.tilSendMeldekortTilBeslutterKommando(
    saksbehandler: Saksbehandler,
): SendMeldekortTilBeslutterKommando {
    val dager = beregning.map { dag ->
        Dager.Dag(
            dag = dag.dato,
            status = when (dag) {
                is MeldeperiodeBeregningDag.IkkeUtfylt -> if (dag.dato.erHelg()) {
                    SendMeldekortTilBeslutterKommando.Status.IKKE_DELTATT
                } else {
                    SendMeldekortTilBeslutterKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
                }

                is MeldeperiodeBeregningDag.Utfylt -> when (dag) {
                    is MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattMedLønnITiltaket -> SendMeldekortTilBeslutterKommando.Status.DELTATT_MED_LØNN_I_TILTAKET
                    is MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattUtenLønnITiltaket -> SendMeldekortTilBeslutterKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
                    is MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SykBruker -> SendMeldekortTilBeslutterKommando.Status.FRAVÆR_SYK
                    is MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SyktBarn -> SendMeldekortTilBeslutterKommando.Status.FRAVÆR_SYKT_BARN
                    is MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdGodkjentAvNav -> SendMeldekortTilBeslutterKommando.Status.FRAVÆR_VELFERD_GODKJENT_AV_NAV
                    is MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdIkkeGodkjentAvNav -> SendMeldekortTilBeslutterKommando.Status.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
                    is MeldeperiodeBeregningDag.Utfylt.IkkeDeltatt -> SendMeldekortTilBeslutterKommando.Status.IKKE_DELTATT
                    is MeldeperiodeBeregningDag.Utfylt.Sperret -> SendMeldekortTilBeslutterKommando.Status.SPERRET
                }
            },
        )
    }.toNonEmptyListOrNull()!!
    return SendMeldekortTilBeslutterKommando(
        sakId = sakId,
        meldekortId = id,
        saksbehandler = saksbehandler,
        dager = Dager(dager),
        correlationId = CorrelationId.generate(),
    )
}
