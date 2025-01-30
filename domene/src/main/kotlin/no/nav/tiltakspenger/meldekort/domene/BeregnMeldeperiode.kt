package no.nav.tiltakspenger.meldekort.domene

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdGodkjentAvNav
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Fravær.Velferd.VelferdIkkeGodkjentAvNav
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.IkkeDeltatt
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeBeregningDag.Utfylt.Sperret
import no.nav.tiltakspenger.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon
import no.nav.tiltakspenger.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon
import no.nav.tiltakspenger.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Status.DELTATT_MED_LØNN_I_TILTAKET
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Status.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Status.FRAVÆR_SYK
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Status.FRAVÆR_SYKT_BARN
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Status.FRAVÆR_VELFERD_GODKJENT_AV_NAV
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Status.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Status.IKKE_DELTATT
import no.nav.tiltakspenger.meldekort.domene.SendMeldekortTilBeslutterKommando.Status.SPERRET
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
        kommando: SendMeldekortTilBeslutterKommando,
        eksisterendeMeldekortPåSaken: MeldekortBehandlinger,
    ): NonEmptyList<MeldeperiodeBeregningDag.Utfylt> {
        require(eksisterendeMeldekortPåSaken.sakId == kommando.sakId) {
            "SakId på eksisterende meldekortperiode ${eksisterendeMeldekortPåSaken.sakId} er ikke likt sakId på kommando ${kommando.sakId}"
        }
        val meldekortSomSkalUtfylles: MeldekortBehandling.MeldekortUnderBehandling =
            eksisterendeMeldekortPåSaken.meldekortUnderBehandling?.also {
                require(it.id == kommando.meldekortId) {
                    "Innsendt meldekort ${kommando.meldekortId} er ikke likt meldekortSomSkalUtfylles ${it.id}"
                }
            } ?: throw IllegalStateException("Fant ingen meldekort som skal utfylles.")

        require(meldekortSomSkalUtfylles.id == kommando.meldekortId) {
            "Innsendt meldekort ${kommando.meldekortId} er ikke likt meldekortSomSkalUtfylles ${meldekortSomSkalUtfylles.id}"
        }

        val meldekortId = kommando.meldekortId
        eksisterendeMeldekortPåSaken.utfylteDager.map { meldekortdag ->
            val tiltakstype = meldekortdag.tiltakstype
            val dag = meldekortdag.dato
            when (meldekortdag) {
                is Sperret -> sperret(meldekortId, tiltakstype, dag, false)
                is VelferdGodkjentAvNav -> gyldigFravær(meldekortId, tiltakstype, dag, false)
                is VelferdIkkeGodkjentAvNav -> ugyldigFravær(meldekortId, tiltakstype, dag, false)
                is SyktBarn -> fraværSykBarn(meldekortId, tiltakstype, dag, false)
                is SykBruker -> fraværSyk(meldekortId, tiltakstype, dag, false)
                is IkkeDeltatt -> ikkeDeltatt(meldekortId, tiltakstype, dag, false)
                is DeltattMedLønnITiltaket -> deltattMedLønn(meldekortId, tiltakstype, dag, false)
                is DeltattUtenLønnITiltaket -> deltattUtenLønn(meldekortId, tiltakstype, dag, false)
            }
        }
        kommando.dager.map { meldekortdag ->
            val tiltakstype = meldekortSomSkalUtfylles.tiltakstype
            val dag = meldekortdag.dag
            when (meldekortdag.status) {
                SPERRET -> sperret(meldekortId, tiltakstype, dag, true)
                DELTATT_UTEN_LØNN_I_TILTAKET -> deltattUtenLønn(meldekortId, tiltakstype, dag, true)
                DELTATT_MED_LØNN_I_TILTAKET -> deltattMedLønn(meldekortId, tiltakstype, dag, true)
                IKKE_DELTATT -> ikkeDeltatt(meldekortId, tiltakstype, dag, true)
                FRAVÆR_SYK -> fraværSyk(meldekortId, tiltakstype, dag, true)
                FRAVÆR_SYKT_BARN -> fraværSykBarn(meldekortId, tiltakstype, dag, true)
                FRAVÆR_VELFERD_GODKJENT_AV_NAV -> gyldigFravær(meldekortId, tiltakstype, dag, true)
                FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> ugyldigFravær(meldekortId, tiltakstype, dag, true)
            }
        }
        return utbetalingDager.toNonEmptyListOrNull()!!
    }

    private fun deltattUtenLønn(
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        skalLeggeTilDag: Boolean,
    ) {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)

        if (skalLeggeTilDag) {
            utbetalingDager.add(
                DeltattUtenLønnITiltaket.create(
                    meldekortId = meldekortId,
                    dato = dag,
                    tiltakstype = tiltakstype,
                ),
            )
        }
    }

    private fun gyldigFravær(
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        skalLeggeTilDag: Boolean,
    ) {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        if (skalLeggeTilDag) {
            utbetalingDager.add(
                VelferdGodkjentAvNav.create(
                    meldekortId = meldekortId,
                    dato = dag,
                    tiltakstype = tiltakstype,
                ),
            )
        }
    }

    private fun ugyldigFravær(
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        skalLeggeTilDag: Boolean,
    ) {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        if (skalLeggeTilDag) {
            utbetalingDager.add(
                VelferdIkkeGodkjentAvNav.create(
                    meldekortId = meldekortId,
                    dato = dag,
                    tiltakstype = tiltakstype,
                ),
            )
        }
    }

    private fun sperret(
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett,
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
                    tiltakstype = tiltakstype,
                ),
            )
        }
    }

    private fun ikkeDeltatt(
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        skalLeggeTilDag: Boolean,
    ) {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        if (skalLeggeTilDag) {
            utbetalingDager.add(
                IkkeDeltatt.create(
                    meldekortId = meldekortId,
                    dato = dag,
                    tiltakstype = tiltakstype,
                ),
            )
        }
    }

    private fun deltattMedLønn(
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        skalLeggeTilDag: Boolean,
    ) {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        if (skalLeggeTilDag) {
            utbetalingDager.add(
                DeltattMedLønnITiltaket.create(
                    meldekortId = meldekortId,
                    dato = dag,
                    tiltakstype = tiltakstype,
                ),
            )
        }
    }

    private fun fraværSyk(
        meldekortId: MeldekortId,
        tiltakstype: TiltakstypeSomGirRett,
        dag: LocalDate,
        skalLeggeTilDag: Boolean,
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
                                dato = dag,
                                tiltakstype = tiltakstype,
                                reduksjon = IngenReduksjon,
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
                                dato = dag,
                                tiltakstype = tiltakstype,
                                reduksjon = Reduksjon,
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
                                dato = dag,
                                tiltakstype = tiltakstype,
                                reduksjon = Reduksjon,
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
                                dato = dag,
                                tiltakstype = tiltakstype,
                                reduksjon = YtelsenFallerBort,
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
                            dato = dag,
                            tiltakstype = tiltakstype,
                            reduksjon = YtelsenFallerBort,
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

fun SendMeldekortTilBeslutterKommando.beregn(
    eksisterendeMeldekortBehandlinger: MeldekortBehandlinger,
): NonEmptyList<MeldeperiodeBeregningDag.Utfylt> {
    return MeldekortBeregning(
        utløsendeMeldekortId = this.meldekortId,
        saksbehandler = this.saksbehandler.navIdent,
    ).beregn(this, eksisterendeMeldekortBehandlinger)
}
