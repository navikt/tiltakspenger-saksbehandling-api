package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdIkkeGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Sperret
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.DELTATT_MED_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.FRAVÆR_SYK
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.FRAVÆR_SYKT_BARN
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.FRAVÆR_VELFERD_GODKJENT_AV_NAV
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.IKKE_DELTATT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.SendMeldekortTilBeslutningKommando.Status.SPERRET
import java.time.LocalDate

private const val ANTALL_EGENMELDINGSDAGER = 3
private const val ANTALL_ARBEIDSGIVERDAGER = 13
private const val DAGER_KARANTENE = 16L - 1

private data class MeldekortBeregning(
    val kommando: SendMeldekortTilBeslutningKommando,
    val eksisterendeMeldekortBehandlinger: MeldekortBehandlinger,
    val barnetilleggsPerioder: Periodisering<AntallBarn?>,
    val tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?>,
) {
    private val meldekortSomSkalUtfylles: MeldekortBehandling

    private var sykTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var egenmeldingsdagerSyk: Int = ANTALL_EGENMELDINGSDAGER
    private var sykKaranteneDag: LocalDate? = null
    private var sisteSykedag: LocalDate? = null

    private var syktBarnTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var egenmeldingsdagerSyktBarn: Int = ANTALL_EGENMELDINGSDAGER
    private var syktBarnKaranteneDag: LocalDate? = null
    private var sisteSyktBarnSykedag: LocalDate? = null

    init {
        val meldekortId = kommando.meldekortId
        val meldekortSomSkalUtfylles = eksisterendeMeldekortBehandlinger.hentMeldekortBehandling(meldekortId)

        require(eksisterendeMeldekortBehandlinger.sakId == kommando.sakId) {
            "SakId på eksisterende meldekortperiode ${eksisterendeMeldekortBehandlinger.sakId} er ikke likt sakId på kommando ${kommando.sakId}"
        }

        requireNotNull(meldekortSomSkalUtfylles) {
            "Fant ikke innsendt meldekort $meldekortId på saken"
        }

        require(meldekortSomSkalUtfylles is MeldekortBehandling.MeldekortUnderBehandling) {
            "Innsendt meldekort $meldekortId er ikke under behandling"
        }

        this.meldekortSomSkalUtfylles = meldekortSomSkalUtfylles
    }

    /** Returnerer beregnede dager fra kommando, og omberegning for relevante dager på saken  */
    fun beregn(): Pair<NonEmptyList<MeldeperiodeBeregningDag.Utfylt>, List<MeldeperiodeOmberegnet>> {
        val oppdatertMeldekortId = meldekortSomSkalUtfylles.id
        val oppdatertKjedeId = meldekortSomSkalUtfylles.kjedeId
        val oppdaterteDager = kommando.dager
        val oppdatertFraOgMed = oppdaterteDager.first().dag

        val (eksisterendeMeldekortFør, eksisterendeMeldekortEtter) = eksisterendeMeldekortBehandlinger.sisteBehandledeMeldekortPerKjede
            .dropWhile { it.kjedeId == oppdatertKjedeId }
            .partition { it.periode.fraOgMed < oppdatertFraOgMed }

        eksisterendeMeldekortFør.forEach { meldekort ->
            meldekort.beregning.dager.forEach { beregnMeldekortDag(it) }
        }

        val oppdaterteDagerBeregnet = oppdaterteDager.map { meldekortdag ->
            val tiltakstype: TiltakstypeSomGirRett by lazy {
                tiltakstypePerioder.hentVerdiForDag(meldekortdag.dag) ?: run {
                    throw IllegalStateException("Fant ingen tiltakstype for dag ${meldekortdag.dag}. tiltakstypeperiode: ${tiltakstypePerioder.totalePeriode}")
                }
            }

            val dag = meldekortdag.dag
            val antallBarn: AntallBarn = barnetilleggsPerioder.hentVerdiForDag(dag) ?: AntallBarn.ZERO

            when (meldekortdag.status) {
                SPERRET -> sperret(dag, oppdatertMeldekortId, oppdatertKjedeId)
                DELTATT_UTEN_LØNN_I_TILTAKET -> deltattUtenLønn(
                    dag,
                    oppdatertMeldekortId,
                    oppdatertKjedeId,
                    tiltakstype,
                    antallBarn,
                )

                DELTATT_MED_LØNN_I_TILTAKET -> deltattMedLønn(
                    dag,
                    oppdatertMeldekortId,
                    oppdatertKjedeId,
                    tiltakstype,
                    antallBarn,
                )

                IKKE_DELTATT -> ikkeDeltatt(dag, oppdatertMeldekortId, oppdatertKjedeId, tiltakstype, antallBarn)
                FRAVÆR_SYK -> fraværSyk(dag, oppdatertMeldekortId, oppdatertKjedeId, tiltakstype, antallBarn)
                FRAVÆR_SYKT_BARN -> fraværSykBarn(dag, oppdatertMeldekortId, oppdatertKjedeId, tiltakstype, antallBarn)
                FRAVÆR_VELFERD_GODKJENT_AV_NAV -> gyldigFravær(
                    dag,
                    oppdatertMeldekortId,
                    oppdatertKjedeId,
                    tiltakstype,
                    antallBarn,
                )

                FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> ugyldigFravær(
                    dag,
                    oppdatertMeldekortId,
                    oppdatertKjedeId,
                    tiltakstype,
                    antallBarn,
                )
            }
        }

        val meldeperioderOmberegnet = eksisterendeMeldekortEtter.map { meldekort ->
            MeldeperiodeOmberegnet(
                periode = meldekort.periode,
                kjedeId = meldekort.kjedeId,
                dager = meldekort.beregning.dager.map { beregnMeldekortDag(it) },
            )
        }

        return oppdaterteDagerBeregnet.toNonEmptyListOrNull()!! to meldeperioderOmberegnet
    }

    private fun beregnMeldekortDag(meldekortdag: MeldeperiodeBeregningDag.Utfylt): MeldeperiodeBeregningDag.Utfylt {
        val tiltakstype: TiltakstypeSomGirRett by lazy {
            meldekortdag.tiltakstype
                ?: throw IllegalStateException("Tidligere meldekortdag.tiltakstype var null for meldekortdag $meldekortdag")
        }

        val dag = meldekortdag.dato
        val meldekortId = meldekortdag.meldekortId
        val kjedeId = meldekortdag.kjedeId
        val antallBarn: AntallBarn = barnetilleggsPerioder.hentVerdiForDag(dag) ?: AntallBarn.ZERO

        val utfyltDag = when (meldekortdag) {
            is Sperret -> sperret(dag, meldekortId, kjedeId)
            is VelferdGodkjentAvNav -> gyldigFravær(dag, meldekortId, kjedeId, tiltakstype, antallBarn)
            is VelferdIkkeGodkjentAvNav -> ugyldigFravær(dag, meldekortId, kjedeId, tiltakstype, antallBarn)
            is SyktBarn -> fraværSykBarn(dag, meldekortId, kjedeId, tiltakstype, antallBarn)
            is SykBruker -> fraværSyk(dag, meldekortId, kjedeId, tiltakstype, antallBarn)
            is IkkeDeltatt -> ikkeDeltatt(dag, meldekortId, kjedeId, tiltakstype, antallBarn)
            is DeltattMedLønnITiltaket -> deltattMedLønn(dag, meldekortId, kjedeId, tiltakstype, antallBarn)
            is DeltattUtenLønnITiltaket -> deltattUtenLønn(dag, meldekortId, kjedeId, tiltakstype, antallBarn)
        }

        return utfyltDag
    }

    private fun deltattUtenLønn(
        dag: LocalDate,
        meldekortId: MeldekortId,
        kjedeId: MeldeperiodeKjedeId,
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ): DeltattUtenLønnITiltaket {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return DeltattUtenLønnITiltaket.create(
            meldekortId = meldekortId,
            kjedeId = kjedeId,
            dato = dag,
            tiltakstype = tiltakstype,
            antallBarn = antallBarn,
        )
    }

    private fun gyldigFravær(
        dag: LocalDate,
        meldekortId: MeldekortId,
        kjedeId: MeldeperiodeKjedeId,
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ): VelferdGodkjentAvNav {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return VelferdGodkjentAvNav.create(
            meldekortId = meldekortId,
            kjedeId = kjedeId,
            dato = dag,
            tiltakstype = tiltakstype,
            antallBarn = antallBarn,
        )
    }

    private fun ugyldigFravær(
        dag: LocalDate,
        meldekortId: MeldekortId,
        kjedeId: MeldeperiodeKjedeId,
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ): VelferdIkkeGodkjentAvNav {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return VelferdIkkeGodkjentAvNav.create(
            meldekortId = meldekortId,
            kjedeId = kjedeId,
            dato = dag,
            tiltakstype = tiltakstype,
            antallBarn = antallBarn,
        )
    }

    private fun sperret(
        dag: LocalDate,
        meldekortId: MeldekortId,
        kjedeId: MeldeperiodeKjedeId,
    ): Sperret {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return Sperret(
            meldekortId = meldekortId,
            kjedeId = kjedeId,
            dato = dag,
        )
    }

    private fun ikkeDeltatt(
        dag: LocalDate,
        meldekortId: MeldekortId,
        kjedeId: MeldeperiodeKjedeId,
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ): IkkeDeltatt {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return IkkeDeltatt.create(
            meldekortId = meldekortId,
            kjedeId = kjedeId,
            dato = dag,
            tiltakstype = tiltakstype,
            antallBarn = antallBarn,
        )
    }

    private fun deltattMedLønn(
        dag: LocalDate,
        meldekortId: MeldekortId,
        kjedeId: MeldeperiodeKjedeId,
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ): DeltattMedLønnITiltaket {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return DeltattMedLønnITiltaket.create(
            meldekortId = meldekortId,
            kjedeId = kjedeId,
            dato = dag,
            tiltakstype = tiltakstype,
            antallBarn = antallBarn,
        )
    }

    private fun fraværSyk(
        dag: LocalDate,
        meldekortId: MeldekortId,
        kjedeId: MeldeperiodeKjedeId,
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ): SykBruker {
        sisteSykedag = dag
        when (sykTilstand) {
            SykTilstand.FullUtbetaling -> {
                if (egenmeldingsdagerSyk > 0) {
                    egenmeldingsdagerSyk--
                    return SykBruker.create(
                        meldekortId = meldekortId,
                        kjedeId = kjedeId,
                        dato = dag,
                        tiltakstype = tiltakstype,
                        reduksjon = IngenReduksjon,
                        antallBarn = antallBarn,
                    )
                } else {
                    egenmeldingsdagerSyk = ANTALL_ARBEIDSGIVERDAGER - 1
                    sykTilstand = SykTilstand.DelvisUtbetaling
                    return SykBruker.create(
                        meldekortId = meldekortId,
                        kjedeId = kjedeId,
                        dato = dag,
                        tiltakstype = tiltakstype,
                        reduksjon = Reduksjon,
                        antallBarn = antallBarn,
                    )
                }
            }

            SykTilstand.DelvisUtbetaling -> {
                if (egenmeldingsdagerSyk > 0) {
                    egenmeldingsdagerSyk--
                    if (egenmeldingsdagerSyk == 0) {
                        sykTilstand = SykTilstand.Karantene
                        sykKaranteneDag = dag.plusDays(DAGER_KARANTENE)
                    }
                    return SykBruker.create(
                        meldekortId = meldekortId,
                        kjedeId = kjedeId,
                        dato = dag,
                        tiltakstype = tiltakstype,
                        reduksjon = Reduksjon,
                        antallBarn = antallBarn,
                    )
                } else {
                    sykTilstand = SykTilstand.Karantene
                    sykKaranteneDag = dag.plusDays(DAGER_KARANTENE)
                    return SykBruker.create(
                        meldekortId = meldekortId,
                        kjedeId = kjedeId,
                        dato = dag,
                        tiltakstype = tiltakstype,
                        reduksjon = YtelsenFallerBort,
                        antallBarn = antallBarn,
                    )
                }
            }

            SykTilstand.Karantene -> {
                sjekkSykKarantene(dag)
                sjekkSykBarnKarantene(dag)
                sykKaranteneDag = dag.plusDays(DAGER_KARANTENE)
                return SykBruker.create(
                    meldekortId = meldekortId,
                    kjedeId = kjedeId,
                    dato = dag,
                    tiltakstype = tiltakstype,
                    reduksjon = YtelsenFallerBort,
                    antallBarn = antallBarn,
                )
            }
        }
    }

    private fun fraværSykBarn(
        dag: LocalDate,
        meldekortId: MeldekortId,
        kjedeId: MeldeperiodeKjedeId,
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ): SyktBarn {
        sisteSykedag = dag
        when (syktBarnTilstand) {
            SykTilstand.FullUtbetaling -> {
                if (egenmeldingsdagerSyktBarn > 0) {
                    egenmeldingsdagerSyktBarn--
                    return SyktBarn.create(
                        meldekortId = meldekortId,
                        kjedeId = kjedeId,
                        dag = dag,
                        tiltakstype = tiltakstype,
                        reduksjon = IngenReduksjon,
                        antallBarn = antallBarn,
                    )
                } else {
                    egenmeldingsdagerSyktBarn = ANTALL_ARBEIDSGIVERDAGER
                    egenmeldingsdagerSyktBarn--
                    syktBarnTilstand = SykTilstand.DelvisUtbetaling
                    return SyktBarn.create(
                        meldekortId = meldekortId,
                        kjedeId = kjedeId,
                        dag = dag,
                        tiltakstype = tiltakstype,
                        reduksjon = Reduksjon,
                        antallBarn = antallBarn,
                    )
                }
            }

            SykTilstand.DelvisUtbetaling -> {
                if (egenmeldingsdagerSyktBarn > 0) {
                    egenmeldingsdagerSyktBarn--
                    if (egenmeldingsdagerSyktBarn == 0) {
                        syktBarnTilstand = SykTilstand.Karantene
                        syktBarnKaranteneDag = dag.plusDays(DAGER_KARANTENE)
                    }
                    return SyktBarn.create(
                        meldekortId = meldekortId,
                        kjedeId = kjedeId,
                        dag = dag,
                        tiltakstype = tiltakstype,
                        reduksjon = Reduksjon,
                        antallBarn = antallBarn,
                    )
                } else {
                    syktBarnTilstand = SykTilstand.Karantene
                    syktBarnKaranteneDag = dag.plusDays(DAGER_KARANTENE)
                    return SyktBarn.create(
                        meldekortId = meldekortId,
                        kjedeId = kjedeId,
                        dag = dag,
                        tiltakstype = tiltakstype,
                        reduksjon = YtelsenFallerBort,
                        antallBarn = antallBarn,
                    )
                }
            }

            SykTilstand.Karantene -> {
                sjekkSykKarantene(dag)
                sjekkSykBarnKarantene(dag)
                return SyktBarn.create(
                    meldekortId = meldekortId,
                    kjedeId = kjedeId,
                    dag = dag,
                    tiltakstype = tiltakstype,
                    reduksjon = YtelsenFallerBort,
                    antallBarn = antallBarn,
                )
            }
        }
    }

    private fun sjekkSykKarantene(dag: LocalDate) {
        if (sisteSykedag != null) {
            if (dag.isAfter(sisteSykedag!!.plusDays(DAGER_KARANTENE))) {
                sykKaranteneDag = null
                egenmeldingsdagerSyk = 3
                sykTilstand = SykTilstand.FullUtbetaling
            }
        }
        if (sykTilstand == SykTilstand.Karantene) {
            if (sykKaranteneDag != null) {
                if (dag.isAfter(sykKaranteneDag)) {
                    sykKaranteneDag = null
                    egenmeldingsdagerSyk = 3
                    sykTilstand = SykTilstand.FullUtbetaling
                }
            }
        }
    }

    private fun sjekkSykBarnKarantene(dag: LocalDate) {
        if (sisteSyktBarnSykedag != null) {
            if (dag.isAfter(sisteSyktBarnSykedag!!.plusDays(DAGER_KARANTENE))) {
                syktBarnKaranteneDag = null
                egenmeldingsdagerSyktBarn = 3
                syktBarnTilstand = SykTilstand.FullUtbetaling
            }
        }
        if (syktBarnTilstand == SykTilstand.Karantene) {
            if (syktBarnKaranteneDag != null) {
                if (dag.isAfter(syktBarnKaranteneDag)) {
                    syktBarnKaranteneDag = null
                    egenmeldingsdagerSyktBarn = 3
                    syktBarnTilstand = SykTilstand.FullUtbetaling
                }
            }
        }
    }
}

private enum class SykTilstand {
    FullUtbetaling,
    DelvisUtbetaling,
    Karantene,
}

fun SendMeldekortTilBeslutningKommando.beregn(
    eksisterendeMeldekortBehandlinger: MeldekortBehandlinger,
    barnetilleggsPerioder: Periodisering<AntallBarn?>,
    tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?>,
) = MeldekortBeregning(
    kommando = this,
    barnetilleggsPerioder = barnetilleggsPerioder,
    tiltakstypePerioder = tiltakstypePerioder,
    eksisterendeMeldekortBehandlinger = eksisterendeMeldekortBehandlinger,
).beregn()
