package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.nonDistinctBy
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
    val eksisterendeMeldekortPåSaken: MeldekortBehandlinger,
    val barnetilleggsPerioder: Periodisering<AntallBarn?>,
    val tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?>,
) {
    private val meldekortId = kommando.meldekortId

    private var sykTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var egenmeldingsdagerSyk: Int = ANTALL_EGENMELDINGSDAGER
    private var sykKaranteneDag: LocalDate? = null
    private var sisteSykedag: LocalDate? = null

    private var syktBarnTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var egenmeldingsdagerSyktBarn: Int = ANTALL_EGENMELDINGSDAGER
    private var syktBarnKaranteneDag: LocalDate? = null
    private var sisteSyktBarnSykedag: LocalDate? = null

    init {
        val meldekortSomSkalUtfylles = eksisterendeMeldekortPåSaken.hentMeldekortBehandling(meldekortId)

        require(eksisterendeMeldekortPåSaken.sakId == kommando.sakId) {
            "SakId på eksisterende meldekortperiode ${eksisterendeMeldekortPåSaken.sakId} er ikke likt sakId på kommando ${kommando.sakId}"
        }

        requireNotNull(meldekortSomSkalUtfylles) {
            "Fant ikke innsendt meldekort $meldekortId på saken"
        }

        require(meldekortSomSkalUtfylles is MeldekortBehandling.MeldekortUnderBehandling) {
            "Innsendt meldekort $meldekortId er ikke under behandling"
        }
    }

    /** Returnerer beregnede dager fra kommando, og (om-)beregning for alle dager på saken  */
    fun beregn(): Pair<NonEmptyList<MeldeperiodeBeregningDag.Utfylt>, List<MeldeperiodeBeregningDag.Utfylt>> {
        val oppdaterteDager = kommando.dager
        val eksisterendeDager = eksisterendeMeldekortPåSaken.utfylteDager
            .filter { eksisterendeDag ->
                !oppdaterteDager.any { it.dag == eksisterendeDag.dato }
            }

        val eksisterendeDagerBeregnet = eksisterendeDager.map { meldekortdag ->
            val tiltakstype: TiltakstypeSomGirRett by lazy {
                meldekortdag.tiltakstype
                    ?: throw IllegalStateException("Tidligere meldekortdag.tiltakstype var null for meldekortdag $meldekortdag")
            }

            val dag = meldekortdag.dato
            val antallBarn: AntallBarn = barnetilleggsPerioder.hentVerdiForDag(dag) ?: AntallBarn.ZERO

            when (meldekortdag) {
                is Sperret -> sperret(dag)
                is VelferdGodkjentAvNav -> gyldigFravær(tiltakstype, dag, antallBarn)
                is VelferdIkkeGodkjentAvNav -> ugyldigFravær(tiltakstype, dag, antallBarn)
                is SyktBarn -> fraværSykBarn(tiltakstype, dag, antallBarn)
                is SykBruker -> fraværSyk(tiltakstype, dag, antallBarn)
                is IkkeDeltatt -> ikkeDeltatt(tiltakstype, dag, antallBarn)
                is DeltattMedLønnITiltaket -> deltattMedLønn(tiltakstype, dag, antallBarn)
                is DeltattUtenLønnITiltaket -> deltattUtenLønn(tiltakstype, dag, antallBarn)
            }
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
                SPERRET -> sperret(dag)
                DELTATT_UTEN_LØNN_I_TILTAKET -> deltattUtenLønn(tiltakstype, dag, antallBarn)
                DELTATT_MED_LØNN_I_TILTAKET -> deltattMedLønn(tiltakstype, dag, antallBarn)
                IKKE_DELTATT -> ikkeDeltatt(tiltakstype, dag, antallBarn)
                FRAVÆR_SYK -> fraværSyk(tiltakstype, dag, antallBarn)
                FRAVÆR_SYKT_BARN -> fraværSykBarn(tiltakstype, dag, antallBarn)
                FRAVÆR_VELFERD_GODKJENT_AV_NAV -> gyldigFravær(tiltakstype, dag, antallBarn)
                FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> ugyldigFravær(tiltakstype, dag, antallBarn)
            }
        }

        val alleDagerBeregnet = (eksisterendeDagerBeregnet + oppdaterteDagerBeregnet).sortedBy { it.dato }

        val duplikateDager = alleDagerBeregnet.nonDistinctBy {
            it.dato
        }

        check(duplikateDager.isEmpty()) {
            "Beregnede utbetalingsdager har duplikate dager - $duplikateDager"
        }

        return oppdaterteDagerBeregnet.toNonEmptyListOrNull()!! to alleDagerBeregnet
    }

    private fun deltattUtenLønn(
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        antallBarn: AntallBarn,
    ): DeltattUtenLønnITiltaket {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return DeltattUtenLønnITiltaket.create(
            meldekortId = meldekortId,
            dato = dag,
            tiltakstype = tiltakstype,
            antallBarn = antallBarn,
        )
    }

    private fun gyldigFravær(
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        antallBarn: AntallBarn,
    ): VelferdGodkjentAvNav {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return VelferdGodkjentAvNav.create(
            meldekortId = meldekortId,
            dato = dag,
            tiltakstype = tiltakstype,
            antallBarn = antallBarn,
        )
    }

    private fun ugyldigFravær(
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        antallBarn: AntallBarn,
    ): VelferdIkkeGodkjentAvNav {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return VelferdIkkeGodkjentAvNav.create(
            meldekortId = meldekortId,
            dato = dag,
            tiltakstype = tiltakstype,
            antallBarn = antallBarn,
        )
    }

    private fun sperret(
        dag: LocalDate,
    ): Sperret {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return Sperret(
            meldekortId = meldekortId,
            dato = dag,
        )
    }

    private fun ikkeDeltatt(
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        antallBarn: AntallBarn,
    ): IkkeDeltatt {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return IkkeDeltatt.create(
            meldekortId = meldekortId,
            dato = dag,
            tiltakstype = tiltakstype,
            antallBarn = antallBarn,
        )
    }

    private fun deltattMedLønn(
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        antallBarn: AntallBarn,
    ): DeltattMedLønnITiltaket {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return DeltattMedLønnITiltaket.create(
            meldekortId = meldekortId,
            dato = dag,
            tiltakstype = tiltakstype,
            antallBarn = antallBarn,
        )
    }

    private fun fraværSyk(
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        antallBarn: AntallBarn,
    ): SykBruker {
        sisteSykedag = dag
        when (sykTilstand) {
            SykTilstand.FullUtbetaling -> {
                if (egenmeldingsdagerSyk > 0) {
                    egenmeldingsdagerSyk--
                    return SykBruker.create(
                        meldekortId = meldekortId,
                        dato = dag,
                        tiltakstype = tiltakstype,
                        reduksjon = IngenReduksjon,
                        antallBarn = antallBarn,
                    )
                } else {
                    egenmeldingsdagerSyk = ANTALL_ARBEIDSGIVERDAGER
                    egenmeldingsdagerSyk--
                    sykTilstand = SykTilstand.DelvisUtbetaling
                    return SykBruker.create(
                        meldekortId = meldekortId,
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
                    dato = dag,
                    tiltakstype = tiltakstype,
                    reduksjon = YtelsenFallerBort,
                    antallBarn = antallBarn,
                )
            }
        }
    }

    private fun fraværSykBarn(
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        antallBarn: AntallBarn,
    ): SyktBarn {
        sisteSykedag = dag
        when (syktBarnTilstand) {
            SykTilstand.FullUtbetaling -> {
                if (egenmeldingsdagerSyktBarn > 0) {
                    egenmeldingsdagerSyktBarn--
                    return SyktBarn.create(
                        meldekortId = meldekortId,
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
    eksisterendeMeldekortPåSaken = eksisterendeMeldekortBehandlinger,
).beregn()
