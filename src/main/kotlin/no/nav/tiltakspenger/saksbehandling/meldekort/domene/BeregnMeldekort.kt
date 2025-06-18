package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Velferd.FraværAnnet
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.Fravær.Velferd.FraværGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregningDag.IkkeRettTilTiltakspenger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.LocalDate

private const val ANTALL_EGENMELDINGSDAGER = 3
private const val ANTALL_ARBEIDSGIVERDAGER = 13
private const val DAGER_KARANTENE = 16L - 1

/**
 * Beregner en gitt meldeperiode, men kan potensielt påvirke påfølgende meldeperioder ved endring av sykefravær.
 *
 * @param meldeperiodeSomBeregnes Kan være en førstegangsberegning, eller en reberegning (korrigering). Dager som saksbehandler eller systemet har fylt ut på meldekortet (kan være basert på innsendt meldekort).
 */
private data class BeregnMeldekort(
    val meldekortIdSomBeregnes: MeldekortId,
    val meldeperiodeSomBeregnes: MeldekortDager,
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
        val meldeperiodeSomBeregnesFraOgMed = meldeperiodeSomBeregnes.first().dato

        val meldeperiodeBeregninger = meldekortBehandlinger.meldeperiodeBeregninger

        return meldekortBehandlinger.sisteBehandledeMeldekortPerKjede
            .filterNot { it.kjedeId == meldeperiodeSomBeregnes.meldeperiode.kjedeId }
            .partition { it.periode.fraOgMed < meldeperiodeSomBeregnesFraOgMed }
            .let { (tidligereMeldekort, påfølgendeMeldekort) ->
                /** Kjør gjennom tidligere meldekort for å sette riktig state for sykedager før vi gjør beregninger på aktuelle meldekort */
                tidligereMeldekort.forEach { beregnEksisterendeMeldekort(it) }

                nonEmptyListOf(
                    MeldeperiodeBeregning(
                        kjedeId = meldeperiodeSomBeregnes.meldeperiode.kjedeId,
                        beregningMeldekortId = meldekortIdSomBeregnes,
                        dagerMeldekortId = meldekortIdSomBeregnes,
                        dager = beregnMeldeperiodeSomBeregnes(),
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
                            beregningMeldekortId = meldekortIdSomBeregnes,
                            dagerMeldekortId = meldekort.id,
                            dager = beregnedeDager,
                        )
                    },
                )
            }
    }

    private fun beregnEksisterendeMeldekort(meldekort: MeldekortBehandling.Behandlet): NonEmptyList<MeldeperiodeBeregningDag> {
        return meldekort.beregning.dagerFraMeldekortet.map {
            beregnDag(
                meldekort.id,
                it.dato,
                it.tilMeldekortDagStatus(),
            ) { it.tiltakstype }
        }.toNonEmptyListOrNull()!!
    }

    private fun beregnMeldeperiodeSomBeregnes(): NonEmptyList<MeldeperiodeBeregningDag> {
        return meldeperiodeSomBeregnes.map {
            beregnDag(
                meldekortIdSomBeregnes,
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
            MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> ikkeRettTilTiltakspenger(dato)
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

            MeldekortDagStatus.IKKE_TILTAKSDAG -> ikkeTiltaksdag(dato, tiltakstype, antallBarn)
            MeldekortDagStatus.FRAVÆR_SYK -> fraværSyk(dato, tiltakstype, antallBarn)
            MeldekortDagStatus.FRAVÆR_SYKT_BARN -> fraværSyktBarn(dato, tiltakstype, antallBarn)
            MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> fraværGodkjentAvNav(
                dato,
                tiltakstype,
                antallBarn,
            )

            MeldekortDagStatus.FRAVÆR_ANNET -> fraværAnnet(
                dato,
                tiltakstype,
                antallBarn,
            )
            MeldekortDagStatus.IKKE_BESVART -> ikkeBesvart(dato, tiltakstype, antallBarn)
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

    private fun fraværGodkjentAvNav(
        dag: LocalDate,
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ): FraværGodkjentAvNav {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return FraværGodkjentAvNav.create(
            dato = dag,
            tiltakstype = tiltakstype,
            antallBarn = antallBarn,
        )
    }

    private fun fraværAnnet(
        dag: LocalDate,
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ): FraværAnnet {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return FraværAnnet.create(
            dato = dag,
            tiltakstype = tiltakstype,
            antallBarn = antallBarn,
        )
    }

    private fun ikkeBesvart(
        dag: LocalDate,
        tiltakstype: TiltakstypeSomGirRett,
        antallBarn: AntallBarn,
    ): MeldeperiodeBeregningDag.IkkeBesvart {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return MeldeperiodeBeregningDag.IkkeBesvart.create(
            dato = dag,
            tiltakstype = tiltakstype,
            antallBarn = antallBarn,
        )
    }

    private fun ikkeRettTilTiltakspenger(
        dag: LocalDate,
    ): IkkeRettTilTiltakspenger {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)
        return IkkeRettTilTiltakspenger(dato = dag)
    }

    private fun ikkeTiltaksdag(
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

    private fun fraværSyktBarn(
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

fun Sak.beregn(
    meldekortIdSomBeregnes: MeldekortId,
    meldeperiodeSomBeregnes: MeldekortDager,
): NonEmptyList<MeldeperiodeBeregning> {
    return beregn(
        meldekortIdSomBeregnes = meldekortIdSomBeregnes,
        meldeperiodeSomBeregnes = meldeperiodeSomBeregnes,
        barnetilleggsPerioder = this.barnetilleggsperioder,
        tiltakstypePerioder = this.tiltakstypeperioder,
        meldekortBehandlinger = meldekortBehandlinger,
    )
}

fun beregn(
    meldekortIdSomBeregnes: MeldekortId,
    meldeperiodeSomBeregnes: MeldekortDager,
    barnetilleggsPerioder: Periodisering<AntallBarn?>,
    tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett?>,
    meldekortBehandlinger: MeldekortBehandlinger,
): NonEmptyList<MeldeperiodeBeregning> {
    return BeregnMeldekort(
        meldekortIdSomBeregnes = meldekortIdSomBeregnes,
        meldeperiodeSomBeregnes = meldeperiodeSomBeregnes,
        barnetilleggsPerioder = barnetilleggsPerioder,
        tiltakstypePerioder = tiltakstypePerioder,
        meldekortBehandlinger = meldekortBehandlinger,
    ).beregn()
}
