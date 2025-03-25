package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.MeldekortId
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
    }

    /** Returnerer beregnede dager fra kommando, og evt omberegninger for relevante meldeperioder på saken */
    fun beregn(): Pair<NonEmptyList<MeldeperiodeBeregningDag.Utfylt>, List<MeldeperiodeOmberegnet>> {
        val oppdatertFraOgMed = kommando.dager.first().dag
        val oppdatertKjedeId = eksisterendeMeldekortBehandlinger
            .hentMeldekortBehandling(kommando.meldekortId)!!
            .kjedeId

        val (eksisterendeMeldekortFør, eksisterendeMeldekortEtter) = eksisterendeMeldekortBehandlinger.sisteBehandledeMeldekortPerKjede
            .dropWhile { it.kjedeId == oppdatertKjedeId }
            .partition { it.periode.fraOgMed < oppdatertFraOgMed }

        /** Tidligere dager må tas høyde for i beregningen, men vil ikke påvirkes av eventuelle fremtidige korrigeringer */
        eksisterendeMeldekortFør.forEach { beregnEksisterendeDager(it.beregning.dager) }

        val oppdaterteDagerBeregnet = beregnOppdaterteDager(kommando)

        /** Dersom dette er en korrigering av en tidligere meldeperiode, kan denne påvirke senere meldeperioder */
        val meldeperioderOmberegnet = eksisterendeMeldekortEtter.mapNotNull { meldekort ->
            val eksisterendeDager = meldekort.beregning.dager
            val oppdaterteDager = beregnEksisterendeDager(eksisterendeDager)

            if (oppdaterteDager == eksisterendeDager) {
                return@mapNotNull null
            }

            MeldeperiodeOmberegnet(
                periode = meldekort.periode,
                kjedeId = meldekort.kjedeId,
                dager = oppdaterteDager,
            )
        }

        return oppdaterteDagerBeregnet to meldeperioderOmberegnet
    }

    private fun beregnEksisterendeDager(dager: List<MeldeperiodeBeregningDag.Utfylt>): NonEmptyList<MeldeperiodeBeregningDag.Utfylt> =
        dager.map { dag ->
            val tiltakstype: TiltakstypeSomGirRett by lazy {
                dag.tiltakstype
                    ?: throw IllegalStateException("Tidligere meldekortdag.tiltakstype var null for meldekortdag $dag")
            }

            val dato = dag.dato
            val meldekortId = dag.meldekortId
            val antallBarn = barnetilleggsPerioder.hentVerdiForDag(dato) ?: AntallBarn.ZERO

            when (dag) {
                is Sperret -> sperret(dato, meldekortId)
                is VelferdGodkjentAvNav -> gyldigFravær(dato, meldekortId, tiltakstype, antallBarn)
                is VelferdIkkeGodkjentAvNav -> ugyldigFravær(dato, meldekortId, tiltakstype, antallBarn)
                is SyktBarn -> fraværSykBarn(dato, meldekortId, tiltakstype, antallBarn)
                is SykBruker -> fraværSyk(dato, meldekortId, tiltakstype, antallBarn)
                is IkkeDeltatt -> ikkeDeltatt(dato, meldekortId, tiltakstype, antallBarn)
                is DeltattMedLønnITiltaket -> deltattMedLønn(dato, meldekortId, tiltakstype, antallBarn)
                is DeltattUtenLønnITiltaket -> deltattUtenLønn(dato, meldekortId, tiltakstype, antallBarn)
            }
        }.toNonEmptyListOrNull()!!

    private fun beregnOppdaterteDager(kommando: SendMeldekortTilBeslutningKommando): NonEmptyList<MeldeperiodeBeregningDag.Utfylt> {
        val meldekortId = kommando.meldekortId

        return kommando.dager.map { dag ->
            val dato = dag.dag

            val tiltakstype: TiltakstypeSomGirRett by lazy {
                tiltakstypePerioder.hentVerdiForDag(dato) ?: run {
                    throw IllegalStateException("Fant ingen tiltakstype for dag $dato. tiltakstypeperiode: ${tiltakstypePerioder.totalePeriode}")
                }
            }

            val antallBarn = barnetilleggsPerioder.hentVerdiForDag(dato) ?: AntallBarn.ZERO

            when (dag.status) {
                SPERRET -> sperret(dato, meldekortId)
                DELTATT_UTEN_LØNN_I_TILTAKET -> deltattUtenLønn(
                    dato,
                    meldekortId,
                    tiltakstype,
                    antallBarn,
                )

                DELTATT_MED_LØNN_I_TILTAKET -> deltattMedLønn(
                    dato,
                    meldekortId,
                    tiltakstype,
                    antallBarn,
                )

                IKKE_DELTATT -> ikkeDeltatt(dato, meldekortId, tiltakstype, antallBarn)
                FRAVÆR_SYK -> fraværSyk(dato, meldekortId, tiltakstype, antallBarn)
                FRAVÆR_SYKT_BARN -> fraværSykBarn(dato, meldekortId, tiltakstype, antallBarn)
                FRAVÆR_VELFERD_GODKJENT_AV_NAV -> gyldigFravær(
                    dato,
                    meldekortId,
                    tiltakstype,
                    antallBarn,
                )

                FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> ugyldigFravær(
                    dato,
                    meldekortId,
                    tiltakstype,
                    antallBarn,
                )
            }
        }.toNonEmptyListOrNull()!!
    }

    private fun deltattUtenLønn(
        dag: LocalDate,
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett,
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
        dag: LocalDate,
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett,
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
        dag: LocalDate,
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett,
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
        meldekortId: MeldekortId,
    ): Sperret {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return Sperret(
            meldekortId = meldekortId,
            dato = dag,
        )
    }

    private fun ikkeDeltatt(
        dag: LocalDate,
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett,
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
        dag: LocalDate,
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett,
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
        dag: LocalDate,
        meldekortId: MeldekortId,
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
        dag: LocalDate,
        meldekortId: MeldekortId,
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
    eksisterendeMeldekortBehandlinger = eksisterendeMeldekortBehandlinger,
).beregn()
