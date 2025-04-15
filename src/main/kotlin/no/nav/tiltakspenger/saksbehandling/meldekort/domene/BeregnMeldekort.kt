package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Velferd.VelferdGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Velferd.VelferdIkkeGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Sperret
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort
import java.time.LocalDate

private const val ANTALL_EGENMELDINGSDAGER = 3
private const val ANTALL_ARBEIDSGIVERDAGER = 13
private const val DAGER_KARANTENE = 16L - 1

private data class BeregnMeldekort(
    val innsendtMeldekortId: MeldekortId,
    val innsendtKjedeId: MeldeperiodeKjedeId,
    val innsendteDager: MeldekortDager,
    val meldekortBehandlinger: MeldekortBehandlinger,
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

    /** Returnerer beregnede dager fra kommando, og evt nye beregninger for påfølgende meldeperioder på saken */
    fun beregn(): NonEmptyList<MeldeperiodeBeregning> {
        val innsendtMeldekortFraOgMed = innsendteDager.first().dato

        val meldeperiodeBeregninger = meldekortBehandlinger.meldeperiodeBeregninger

        return meldekortBehandlinger.sisteBehandledeMeldekortPerKjede
            .filterNot { it.kjedeId == innsendtKjedeId }
            .partition { it.periode.fraOgMed < innsendtMeldekortFraOgMed }
            .let { (tidligereMeldekort, påfølgendeMeldekort) ->
                /** Kjør gjennom tidligere meldekort for å sette riktig state for sykedager før vi gjør beregninger på aktuelle meldekort */
                tidligereMeldekort.forEach { beregnEksisterendeMeldekort(it) }

                nonEmptyListOf(
                    MeldeperiodeBeregning(
                        kjedeId = innsendtKjedeId,
                        beregningMeldekortId = innsendtMeldekortId,
                        dagerMeldekortId = innsendtMeldekortId,
                        dager = beregnInnsendteDager(),
                    ),
                ).plus(
                    /** Dersom meldekort-behandlingen er en korrigering tilbake i tid, kan utbetalinger for påfølgende meldekort potensielt
                     *  endres som følge av sykedager reglene.
                     * */
                    påfølgendeMeldekort.mapNotNull { meldekort ->
                        val kjedeId = meldekort.kjedeId

                        val beregnedeDager = beregnEksisterendeMeldekort(meldekort)
                        val forrigeBeregning = meldeperiodeBeregninger.sisteBeregningForKjede[kjedeId]

                        if (beregnedeDager == forrigeBeregning?.dager) {
                            return@mapNotNull null
                        }

                        MeldeperiodeBeregning(
                            kjedeId = kjedeId,
                            beregningMeldekortId = innsendtMeldekortId,
                            dagerMeldekortId = meldekort.id,
                            dager = beregnedeDager,
                        )
                    },
                )
            }
    }

    private fun beregnEksisterendeMeldekort(meldekort: MeldekortBehandletManuelt): NonEmptyList<MeldeperiodeBeregningDag> {
        return meldekort.beregning.dagerFraMeldekortet.map {
            beregnDag(
                meldekort.id,
                it.dato,
                it.tilMeldekortDagStatus(),
            ) { it.tiltakstype }
        }.toNonEmptyListOrNull()!!
    }

    private fun beregnInnsendteDager(): NonEmptyList<MeldeperiodeBeregningDag> {
        return innsendteDager.map {
            beregnDag(
                innsendtMeldekortId,
                it.dato,
                it.status,
            ) { tiltakstypePerioder.hentVerdiForDag(it.dato) }
        }.toNonEmptyListOrNull()!!
    }

    private fun beregnDag(
        meldekortId: MeldekortId,
        dato: LocalDate,
        status: MeldekortDagStatus,
        hentTiltakstype: () -> TiltakstypeSomGirRett?,
    ): MeldeperiodeBeregningDag {
        val antallBarn = barnetilleggsPerioder.hentVerdiForDag(dato) ?: AntallBarn.ZERO

        val tiltakstype by lazy {
            hentTiltakstype() ?: run {
                throw IllegalStateException(
                    "Fant ingen tiltakstype for dag $dato for meldekort $meldekortId",
                )
            }
        }

        return when (status) {
            MeldekortDagStatus.SPERRET -> sperret(dato)
            MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> deltattUtenLønn(
                dato,
                tiltakstype,
                antallBarn,
            )

            MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> deltattMedLønn(
                dato,
                tiltakstype,
                antallBarn,
            )

            MeldekortDagStatus.IKKE_DELTATT -> ikkeDeltatt(dato, tiltakstype, antallBarn)
            MeldekortDagStatus.FRAVÆR_SYK -> fraværSyk(dato, tiltakstype, antallBarn)
            MeldekortDagStatus.FRAVÆR_SYKT_BARN -> fraværSykBarn(dato, tiltakstype, antallBarn)
            MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV -> gyldigFravær(
                dato,
                tiltakstype,
                antallBarn,
            )

            MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> ugyldigFravær(
                dato,
                tiltakstype,
                antallBarn,
            )

            MeldekortDagStatus.IKKE_UTFYLT -> throw IllegalStateException("Alle dager på meldekortet må være utfylt - $dato var ikke utfylt på $meldekortId")
        }
    }

    private fun deltattUtenLønn(
        dag: LocalDate,
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ): DeltattUtenLønnITiltaket {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return DeltattUtenLønnITiltaket.create(
            dato = dag,
            tiltakstype = tiltakstype,
            antallBarn = antallBarn,
        )
    }

    private fun gyldigFravær(
        dag: LocalDate,
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ): VelferdGodkjentAvNav {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return VelferdGodkjentAvNav.create(
            dato = dag,
            tiltakstype = tiltakstype,
            antallBarn = antallBarn,
        )
    }

    private fun ugyldigFravær(
        dag: LocalDate,
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ): VelferdIkkeGodkjentAvNav {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return VelferdIkkeGodkjentAvNav.create(
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
        return Sperret(dato = dag)
    }

    private fun ikkeDeltatt(
        dag: LocalDate,
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ): IkkeDeltatt {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return IkkeDeltatt.create(
            dato = dag,
            tiltakstype = tiltakstype,
            antallBarn = antallBarn,
        )
    }

    private fun deltattMedLønn(
        dag: LocalDate,
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ): DeltattMedLønnITiltaket {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return DeltattMedLønnITiltaket.create(
            dato = dag,
            tiltakstype = tiltakstype,
            antallBarn = antallBarn,
        )
    }

    private fun fraværSyk(
        dag: LocalDate,
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ): SykBruker {
        sisteSykedag = dag
        when (sykTilstand) {
            SykTilstand.FullUtbetaling -> {
                if (egenmeldingsdagerSyk > 0) {
                    egenmeldingsdagerSyk--
                    return SykBruker.create(
                        dato = dag,
                        tiltakstype = tiltakstype,
                        reduksjon = IngenReduksjon,
                        antallBarn = antallBarn,
                    )
                } else {
                    egenmeldingsdagerSyk = ANTALL_ARBEIDSGIVERDAGER - 1
                    sykTilstand = SykTilstand.DelvisUtbetaling
                    return SykBruker.create(
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
                        dato = dag,
                        tiltakstype = tiltakstype,
                        reduksjon = Reduksjon,
                        antallBarn = antallBarn,
                    )
                } else {
                    sykTilstand = SykTilstand.Karantene
                    sykKaranteneDag = dag.plusDays(DAGER_KARANTENE)
                    return SykBruker.create(
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
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ): SyktBarn {
        sisteSykedag = dag
        when (syktBarnTilstand) {
            SykTilstand.FullUtbetaling -> {
                if (egenmeldingsdagerSyktBarn > 0) {
                    egenmeldingsdagerSyktBarn--
                    return SyktBarn.create(
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
                        dag = dag,
                        tiltakstype = tiltakstype,
                        reduksjon = Reduksjon,
                        antallBarn = antallBarn,
                    )
                } else {
                    syktBarnTilstand = SykTilstand.Karantene
                    syktBarnKaranteneDag = dag.plusDays(DAGER_KARANTENE)
                    return SyktBarn.create(
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

fun OppdaterMeldekortKommando.beregn(
    meldekortBehandlinger: MeldekortBehandlinger,
    barnetilleggsPerioder: Periodisering<AntallBarn?>,
    tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?>,
): NonEmptyList<MeldeperiodeBeregning> {
    val meldekortId = this.meldekortId

    require(meldekortBehandlinger.sakId == this.sakId) {
        "SakId på eksisterende meldekortperiode ${meldekortBehandlinger.sakId} er ikke likt sakId på kommando $sakId"
    }

    val meldekortSomBehandles = meldekortBehandlinger.hentMeldekortBehandling(meldekortId)

    require(meldekortSomBehandles is MeldekortUnderBehandling) {
        "Innsendt meldekort $meldekortId er ikke under behandling"
    }

    return BeregnMeldekort(
        innsendtMeldekortId = this.meldekortId,
        innsendtKjedeId = meldekortSomBehandles.kjedeId,
        innsendteDager = this.dager.tilMeldekortDager(meldekortSomBehandles.meldeperiode.antallDagerSomGirRett),
        barnetilleggsPerioder = barnetilleggsPerioder,
        tiltakstypePerioder = tiltakstypePerioder,
        meldekortBehandlinger = meldekortBehandlinger,
    ).beregn()
}

fun BrukersMeldekort.beregn(
    meldekortBehandlingId: MeldekortId,
    meldekortBehandlinger: MeldekortBehandlinger,
    barnetilleggsPerioder: Periodisering<AntallBarn?>,
    tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?>,
): NonEmptyList<MeldeperiodeBeregning> = BeregnMeldekort(
    innsendtMeldekortId = meldekortBehandlingId,
    innsendtKjedeId = this.kjedeId,
    innsendteDager = this.tilMeldekortDager(),
    barnetilleggsPerioder = barnetilleggsPerioder,
    tiltakstypePerioder = tiltakstypePerioder,
    meldekortBehandlinger = meldekortBehandlinger,
).beregn()
