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
    private val utbetalingDager = mutableListOf<MeldeperiodeBeregningDag.Utfylt>()

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

    fun beregn(): NonEmptyList<MeldeperiodeBeregningDag.Utfylt> {
        val oppdaterteDager = kommando.dager
        val eksisterendeDager = eksisterendeMeldekortPåSaken.utfylteDager
            .filter { eksisterendeDag ->
                !oppdaterteDager.any { it.dag == eksisterendeDag.dato }
            }

        eksisterendeDager.forEach { meldekortdag ->
            val tiltakstype: TiltakstypeSomGirRett by lazy {
                meldekortdag.tiltakstype
                    ?: throw IllegalStateException("Tidligere meldekortdag.tiltakstype var null for meldekortdag $meldekortdag")
            }

            val dag = meldekortdag.dato
            val antallBarn: AntallBarn = barnetilleggsPerioder.hentVerdiForDag(dag) ?: AntallBarn.ZERO

            when (meldekortdag) {
                is Sperret -> sperret(dag, false)
                is VelferdGodkjentAvNav -> gyldigFravær(tiltakstype, dag, false, antallBarn)
                is VelferdIkkeGodkjentAvNav -> ugyldigFravær(tiltakstype, dag, false, antallBarn)
                is SyktBarn -> fraværSykBarn(tiltakstype, dag, false, antallBarn)
                is SykBruker -> fraværSyk(tiltakstype, dag, false, antallBarn)
                is IkkeDeltatt -> ikkeDeltatt(tiltakstype, dag, false, antallBarn)
                is DeltattMedLønnITiltaket -> deltattMedLønn(tiltakstype, dag, false, antallBarn)
                is DeltattUtenLønnITiltaket -> deltattUtenLønn(tiltakstype, dag, false, antallBarn)
            }
        }

        oppdaterteDager.forEach { meldekortdag ->
            val tiltakstype: TiltakstypeSomGirRett by lazy {
                tiltakstypePerioder.hentVerdiForDag(meldekortdag.dag) ?: run {
                    throw IllegalStateException("Fant ingen tiltakstype for dag ${meldekortdag.dag}. tiltakstypeperiode: ${tiltakstypePerioder.totalePeriode}")
                }
            }

            val dag = meldekortdag.dag
            val antallBarn: AntallBarn = barnetilleggsPerioder.hentVerdiForDag(dag) ?: AntallBarn.ZERO

            when (meldekortdag.status) {
                SPERRET -> sperret(dag, true)
                DELTATT_UTEN_LØNN_I_TILTAKET -> deltattUtenLønn(tiltakstype, dag, true, antallBarn)
                DELTATT_MED_LØNN_I_TILTAKET -> deltattMedLønn(tiltakstype, dag, true, antallBarn)
                IKKE_DELTATT -> ikkeDeltatt(tiltakstype, dag, true, antallBarn)
                FRAVÆR_SYK -> fraværSyk(tiltakstype, dag, true, antallBarn)
                FRAVÆR_SYKT_BARN -> fraværSykBarn(tiltakstype, dag, true, antallBarn)
                FRAVÆR_VELFERD_GODKJENT_AV_NAV -> gyldigFravær(tiltakstype, dag, true, antallBarn)
                FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> ugyldigFravær(tiltakstype, dag, true, antallBarn)
            }
        }

        val duplikateDager = utbetalingDager.nonDistinctBy {
            it.dato
        }

        check(duplikateDager.isEmpty()) {
            "Utbetalingsdagene har duplikate dager - $duplikateDager"
        }

        return utbetalingDager.toNonEmptyListOrNull()!!
    }

    private fun deltattUtenLønn(
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        skalLeggeTilDag: Boolean,
        antallBarn: AntallBarn,
    ) {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)

        if (skalLeggeTilDag) {
            utbetalingDager.add(
                DeltattUtenLønnITiltaket.create(
                    meldekortId = meldekortId,
                    dato = dag,
                    tiltakstype = tiltakstype,
                    antallBarn = antallBarn,
                ),
            )
        }
    }

    private fun gyldigFravær(
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        skalLeggeTilDag: Boolean,
        antallBarn: AntallBarn,
    ) {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        if (skalLeggeTilDag) {
            utbetalingDager.add(
                VelferdGodkjentAvNav.create(
                    meldekortId = meldekortId,
                    dato = dag,
                    tiltakstype = tiltakstype,
                    antallBarn = antallBarn,
                ),
            )
        }
    }

    private fun ugyldigFravær(
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        skalLeggeTilDag: Boolean,
        antallBarn: AntallBarn,
    ) {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        if (skalLeggeTilDag) {
            utbetalingDager.add(
                VelferdIkkeGodkjentAvNav.create(
                    meldekortId = meldekortId,
                    dato = dag,
                    tiltakstype = tiltakstype,
                    antallBarn = antallBarn,
                ),
            )
        }
    }

    private fun sperret(
        dag: LocalDate,
        skalLeggeTilDag: Boolean,
    ) {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        if (skalLeggeTilDag) {
            utbetalingDager.add(
                Sperret(
                    meldekortId = meldekortId,
                    dato = dag,
                ),
            )
        }
    }

    private fun ikkeDeltatt(
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        skalLeggeTilDag: Boolean,
        antallBarn: AntallBarn,
    ) {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        if (skalLeggeTilDag) {
            utbetalingDager.add(
                IkkeDeltatt.create(
                    meldekortId = meldekortId,
                    dato = dag,
                    tiltakstype = tiltakstype,
                    antallBarn = antallBarn,
                ),
            )
        }
    }

    private fun deltattMedLønn(
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        skalLeggeTilDag: Boolean,
        antallBarn: AntallBarn,
    ) {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        if (skalLeggeTilDag) {
            utbetalingDager.add(
                DeltattMedLønnITiltaket.create(
                    meldekortId = meldekortId,
                    dato = dag,
                    tiltakstype = tiltakstype,
                    antallBarn = antallBarn,
                ),
            )
        }
    }

    private fun fraværSyk(
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        skalLeggeTilDag: Boolean,
        antallBarn: AntallBarn,
    ) {
        sisteSykedag = dag
        when (sykTilstand) {
            SykTilstand.FullUtbetaling -> {
                if (egenmeldingsdagerSyk > 0) {
                    egenmeldingsdagerSyk--
                    if (skalLeggeTilDag) {
                        utbetalingDager.add(
                            SykBruker.create(
                                meldekortId = meldekortId,
                                dato = dag,
                                tiltakstype = tiltakstype,
                                reduksjon = IngenReduksjon,
                                antallBarn = antallBarn,
                            ),
                        )
                    }
                } else {
                    egenmeldingsdagerSyk = ANTALL_ARBEIDSGIVERDAGER
                    egenmeldingsdagerSyk--
                    sykTilstand = SykTilstand.DelvisUtbetaling
                    if (skalLeggeTilDag) {
                        utbetalingDager.add(
                            SykBruker.create(
                                meldekortId = meldekortId,
                                dato = dag,
                                tiltakstype = tiltakstype,
                                reduksjon = Reduksjon,
                                antallBarn = antallBarn,
                            ),
                        )
                    }
                }
            }

            SykTilstand.DelvisUtbetaling -> {
                if (egenmeldingsdagerSyk > 0) {
                    egenmeldingsdagerSyk--
                    if (skalLeggeTilDag) {
                        utbetalingDager.add(
                            SykBruker.create(
                                meldekortId = meldekortId,
                                dato = dag,
                                tiltakstype = tiltakstype,
                                reduksjon = Reduksjon,
                                antallBarn = antallBarn,
                            ),
                        )
                    }
                    if (egenmeldingsdagerSyk == 0) {
                        sykTilstand = SykTilstand.Karantene
                        sykKaranteneDag = dag.plusDays(DAGER_KARANTENE)
                    }
                } else {
                    sykTilstand = SykTilstand.Karantene
                    sykKaranteneDag = dag.plusDays(DAGER_KARANTENE)
                    if (skalLeggeTilDag) {
                        utbetalingDager.add(
                            SykBruker.create(
                                meldekortId = meldekortId,
                                dato = dag,
                                tiltakstype = tiltakstype,
                                reduksjon = YtelsenFallerBort,
                                antallBarn = antallBarn,
                            ),
                        )
                    }
                }
            }

            SykTilstand.Karantene -> {
                sjekkSykKarantene(dag)
                sjekkSykBarnKarantene(dag)
                sykKaranteneDag = dag.plusDays(DAGER_KARANTENE)
                if (skalLeggeTilDag) {
                    utbetalingDager.add(
                        SykBruker.create(
                            meldekortId = meldekortId,
                            dato = dag,
                            tiltakstype = tiltakstype,
                            reduksjon = YtelsenFallerBort,
                            antallBarn = antallBarn,
                        ),
                    )
                }
            }
        }
    }

    private fun fraværSykBarn(
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        skalLeggeTilDag: Boolean,
        antallBarn: AntallBarn,
    ) {
        sisteSykedag = dag
        when (syktBarnTilstand) {
            SykTilstand.FullUtbetaling -> {
                if (egenmeldingsdagerSyktBarn > 0) {
                    egenmeldingsdagerSyktBarn--
                    if (skalLeggeTilDag) {
                        utbetalingDager.add(
                            SyktBarn.create(
                                meldekortId = meldekortId,
                                dag = dag,
                                tiltakstype = tiltakstype,
                                reduksjon = IngenReduksjon,
                                antallBarn = antallBarn,
                            ),
                        )
                    }
                } else {
                    egenmeldingsdagerSyktBarn = ANTALL_ARBEIDSGIVERDAGER
                    egenmeldingsdagerSyktBarn--
                    syktBarnTilstand = SykTilstand.DelvisUtbetaling
                    if (skalLeggeTilDag) {
                        utbetalingDager.add(
                            SyktBarn.create(
                                meldekortId = meldekortId,
                                dag = dag,
                                tiltakstype = tiltakstype,
                                reduksjon = Reduksjon,
                                antallBarn = antallBarn,
                            ),
                        )
                    }
                }
            }

            SykTilstand.DelvisUtbetaling -> {
                if (egenmeldingsdagerSyktBarn > 0) {
                    egenmeldingsdagerSyktBarn--
                    if (skalLeggeTilDag) {
                        utbetalingDager.add(
                            SyktBarn.create(
                                meldekortId = meldekortId,
                                dag = dag,
                                tiltakstype = tiltakstype,
                                reduksjon = Reduksjon,
                                antallBarn = antallBarn,
                            ),
                        )
                    }
                    if (egenmeldingsdagerSyktBarn == 0) {
                        syktBarnTilstand = SykTilstand.Karantene
                        syktBarnKaranteneDag = dag.plusDays(DAGER_KARANTENE)
                    }
                } else {
                    syktBarnTilstand = SykTilstand.Karantene
                    syktBarnKaranteneDag = dag.plusDays(DAGER_KARANTENE)
                    if (skalLeggeTilDag) {
                        utbetalingDager.add(
                            SyktBarn.create(
                                meldekortId = meldekortId,
                                dag = dag,
                                tiltakstype = tiltakstype,
                                reduksjon = YtelsenFallerBort,
                                antallBarn = antallBarn,
                            ),
                        )
                    }
                }
            }

            SykTilstand.Karantene -> {
                sjekkSykKarantene(dag)
                sjekkSykBarnKarantene(dag)
                if (skalLeggeTilDag) {
                    utbetalingDager.add(
                        SyktBarn.create(
                            meldekortId = meldekortId,
                            dag = dag,
                            tiltakstype = tiltakstype,
                            reduksjon = YtelsenFallerBort,
                            antallBarn = antallBarn,
                        ),
                    )
                }
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
): NonEmptyList<MeldeperiodeBeregningDag.Utfylt> {
    return MeldekortBeregning(
        kommando = this,
        barnetilleggsPerioder = barnetilleggsPerioder,
        tiltakstypePerioder = tiltakstypePerioder,
        eksisterendeMeldekortPåSaken = eksisterendeMeldekortBehandlinger,
    ).beregn()
}
