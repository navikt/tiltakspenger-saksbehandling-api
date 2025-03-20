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
    val utløsendeMeldekortId: MeldekortId,
    val utbetalingDager: MutableList<MeldeperiodeBeregningDag.Utfylt> = mutableListOf(),
    val saksbehandler: String,
) {
    private var sykTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var egenmeldingsdagerSyk: Int = ANTALL_EGENMELDINGSDAGER
    private var sykKaranteneDag: LocalDate? = null
    private var sisteSykedag: LocalDate? = null

    private var syktBarnTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var egenmeldingsdagerSyktBarn: Int = ANTALL_EGENMELDINGSDAGER
    private var syktBarnKaranteneDag: LocalDate? = null
    private var sisteSyktBarnSykedag: LocalDate? = null

    fun beregn(
        kommando: SendMeldekortTilBeslutningKommando,
        eksisterendeMeldekortPåSaken: MeldekortBehandlinger,
        barnetilleggsPerioder: Periodisering<AntallBarn?>,
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?>,
    ): NonEmptyList<MeldeperiodeBeregningDag.Utfylt> {
        val meldekortId = kommando.meldekortId

        require(eksisterendeMeldekortPåSaken.sakId == kommando.sakId) {
            "SakId på eksisterende meldekortperiode ${eksisterendeMeldekortPåSaken.sakId} er ikke likt sakId på kommando ${kommando.sakId}"
        }

        eksisterendeMeldekortPåSaken.utfylteDager.forEach {
            require(it.tiltakstype != null) {
                "Tidligere meldekortdag.tiltakstype var null for meldekortdag $it"
            }
        }

        val meldekortSomSkalUtfylles = eksisterendeMeldekortPåSaken.hentMeldekortBehandling(meldekortId)

        requireNotNull(meldekortSomSkalUtfylles) {
            "Fant ikke innsendt meldekort $meldekortId på saken"
        }
        require(meldekortSomSkalUtfylles is MeldekortBehandling.MeldekortUnderBehandling) {
            "Innsendt meldekort $meldekortId er ikke under behandling"
        }

        if (meldekortSomSkalUtfylles.type == MeldekortBehandlingType.KORRIGERING) {
            return beregnKorrigering(kommando, eksisterendeMeldekortPåSaken, barnetilleggsPerioder, tiltakstypePerioder)
        }

        eksisterendeMeldekortPåSaken.utfylteDager.map { meldekortdag ->
            // Vi ønsker ikke endre tidligere utfylte dager.
            val tiltakstype: TiltakstypeSomGirRett by lazy {
                meldekortdag.tiltakstype
                    ?: throw IllegalStateException("Tidligere meldekortdag.tiltakstype var null for meldekortdag $meldekortdag")
            }

            val dag = meldekortdag.dato
            val antallBarn: AntallBarn = barnetilleggsPerioder.hentVerdiForDag(dag) ?: AntallBarn.ZERO
            when (meldekortdag) {
                is Sperret -> sperret(meldekortId, dag, false)
                is VelferdGodkjentAvNav -> gyldigFravær(meldekortId, tiltakstype, dag, false, antallBarn)
                is VelferdIkkeGodkjentAvNav -> ugyldigFravær(meldekortId, tiltakstype, dag, false, antallBarn)
                is SyktBarn -> fraværSykBarn(meldekortId, tiltakstype, dag, false, antallBarn)
                is SykBruker -> fraværSyk(meldekortId, tiltakstype, dag, false, antallBarn)
                is IkkeDeltatt -> ikkeDeltatt(meldekortId, tiltakstype, dag, false, antallBarn)
                is DeltattMedLønnITiltaket -> deltattMedLønn(meldekortId, tiltakstype, dag, false, antallBarn)
                is DeltattUtenLønnITiltaket -> deltattUtenLønn(meldekortId, tiltakstype, dag, false, antallBarn)
            }
        }
        kommando.dager.map { meldekortdag ->
            val tiltakstype: TiltakstypeSomGirRett by lazy {
                tiltakstypePerioder.hentVerdiForDag(meldekortdag.dag) ?: run {
                    throw IllegalStateException("Fant ingen tiltakstype for dag ${meldekortdag.dag}. tiltakstypeperiode: ${tiltakstypePerioder.totalePeriode}")
                }
            }
            val dag = meldekortdag.dag
            val antallBarn: AntallBarn = barnetilleggsPerioder.hentVerdiForDag(dag) ?: AntallBarn.ZERO

            when (meldekortdag.status) {
                SPERRET -> sperret(meldekortId, dag, true)
                DELTATT_UTEN_LØNN_I_TILTAKET -> deltattUtenLønn(meldekortId, tiltakstype, dag, true, antallBarn)
                DELTATT_MED_LØNN_I_TILTAKET -> deltattMedLønn(meldekortId, tiltakstype, dag, true, antallBarn)
                IKKE_DELTATT -> ikkeDeltatt(meldekortId, tiltakstype, dag, true, antallBarn)
                FRAVÆR_SYK -> fraværSyk(meldekortId, tiltakstype, dag, true, antallBarn)
                FRAVÆR_SYKT_BARN -> fraværSykBarn(meldekortId, tiltakstype, dag, true, antallBarn)
                FRAVÆR_VELFERD_GODKJENT_AV_NAV -> gyldigFravær(meldekortId, tiltakstype, dag, true, antallBarn)
                FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> ugyldigFravær(meldekortId, tiltakstype, dag, true, antallBarn)
            }
        }
        return utbetalingDager.toNonEmptyListOrNull()!!
    }

    private fun beregnKorrigering(
        kommando: SendMeldekortTilBeslutningKommando,
        eksisterendeMeldekortPåSaken: MeldekortBehandlinger,
        barnetilleggsPerioder: Periodisering<AntallBarn>,
        tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett>,
    ): NonEmptyList<MeldeperiodeBeregningDag.Utfylt> {
        val meldekortId = kommando.meldekortId
        val eksisterendeDager = eksisterendeMeldekortPåSaken.utfylteDager
        val oppdaterteDager = kommando.dager

        eksisterendeDager
            .filter { eksisterendeDag -> !oppdaterteDager.any { it.dag == eksisterendeDag.dato } }
            .forEach { meldekortdag ->
                val tiltakstype = meldekortdag.tiltakstype!!

                val dag = meldekortdag.dato
                val antallBarn: AntallBarn = barnetilleggsPerioder.hentVerdiForDag(dag) ?: AntallBarn.ZERO
                when (meldekortdag) {
                    is Sperret -> sperret(meldekortId, dag, false)
                    is VelferdGodkjentAvNav -> gyldigFravær(meldekortId, tiltakstype, dag, false, antallBarn)
                    is VelferdIkkeGodkjentAvNav -> ugyldigFravær(meldekortId, tiltakstype, dag, false, antallBarn)
                    is SyktBarn -> fraværSykBarn(meldekortId, tiltakstype, dag, false, antallBarn)
                    is SykBruker -> fraværSyk(meldekortId, tiltakstype, dag, false, antallBarn)
                    is IkkeDeltatt -> ikkeDeltatt(meldekortId, tiltakstype, dag, false, antallBarn)
                    is DeltattMedLønnITiltaket -> deltattMedLønn(meldekortId, tiltakstype, dag, false, antallBarn)
                    is DeltattUtenLønnITiltaket -> deltattUtenLønn(meldekortId, tiltakstype, dag, false, antallBarn)
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
                SPERRET -> sperret(meldekortId, dag, true)
                DELTATT_UTEN_LØNN_I_TILTAKET -> deltattUtenLønn(meldekortId, tiltakstype, dag, true, antallBarn)
                DELTATT_MED_LØNN_I_TILTAKET -> deltattMedLønn(meldekortId, tiltakstype, dag, true, antallBarn)
                IKKE_DELTATT -> ikkeDeltatt(meldekortId, tiltakstype, dag, true, antallBarn)
                FRAVÆR_SYK -> fraværSyk(meldekortId, tiltakstype, dag, true, antallBarn)
                FRAVÆR_SYKT_BARN -> fraværSykBarn(meldekortId, tiltakstype, dag, true, antallBarn)
                FRAVÆR_VELFERD_GODKJENT_AV_NAV -> gyldigFravær(meldekortId, tiltakstype, dag, true, antallBarn)
                FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> ugyldigFravær(meldekortId, tiltakstype, dag, true, antallBarn)
            }
        }
        return utbetalingDager.toNonEmptyListOrNull()!!
    }

    private fun deltattUtenLønn(
        meldekortId: MeldekortId,
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
        meldekortId: MeldekortId,
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
        meldekortId: MeldekortId,
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
        meldekortId: MeldekortId,
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
        meldekortId: MeldekortId,
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
        meldekortId: MeldekortId,
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
        meldekortId: MeldekortId,
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
        meldekortId: MeldekortId,
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
        utløsendeMeldekortId = this.meldekortId,
        saksbehandler = this.saksbehandler.navIdent,
    ).beregn(this, eksisterendeMeldekortBehandlinger, barnetilleggsPerioder, tiltakstypePerioder)
}
