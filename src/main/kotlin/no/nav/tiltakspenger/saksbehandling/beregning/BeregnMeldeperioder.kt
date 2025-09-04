package no.nav.tiltakspenger.saksbehandling.beregning

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Velferd.FraværAnnet
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Velferd.FraværGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.IkkeRettTilTiltakspenger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDager
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.tilMeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.LocalDate

private const val ANTALL_EGENMELDINGSDAGER = 3
private const val ANTALL_ARBEIDSGIVERDAGER = 13
private const val DAGER_KARANTENE = 16L - 1

/**
 * Beregner en eller flere meldeperioder, men kan potensielt påvirke påfølgende meldeperioder ved endring av sykefravær.
 *
 * @param meldeperioderSomBeregnes Dager som saksbehandler eller systemet har fylt ut for en eller flere meldeperioder.
 * Kan være basert på innsendt meldekort, manuell meldekortbehandling eller revurdering over tidligere utbetalte perioder.
 */
private data class BeregnMeldeperioder(
    val beregningKilde: BeregningKilde,
    val meldeperioderSomBeregnes: NonEmptyList<MeldeperiodeSomSkalBeregnes>,
    val barnetilleggsPerioder: Periodisering<AntallBarn>,
    val tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett>,
    val meldeperiodeBeregninger: MeldeperiodeBeregninger,
) {
    private var sykTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var egenmeldingsdagerSyk: Int = ANTALL_EGENMELDINGSDAGER
    private var sykKaranteneDag: LocalDate? = null
    private var sisteSykedag: LocalDate? = null

    private var syktBarnTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var egenmeldingsdagerSyktBarn: Int = ANTALL_EGENMELDINGSDAGER
    private var syktBarnKaranteneDag: LocalDate? = null
    private var sisteSyktBarnSykedag: LocalDate? = null

    /** Returnerer nye eller endrede beregninger for meldeperioder. Returnerer tom liste dersom ingen beregninger endres */
    fun beregn(): List<MeldeperiodeBeregning> {
        val meldeperioderSomBeregnesFraOgMed = meldeperioderSomBeregnes.first().fraOgMed

        return meldeperiodeBeregninger.gjeldendeBeregninger
            .partition { it.fraOgMed < meldeperioderSomBeregnesFraOgMed }
            .let { (tidligereBeregninger, påfølgendeBeregninger) ->
                /** Kjør gjennom tidligere beregninger for å sette riktig state for sykedager før vi gjør nye beregninger */
                tidligereBeregninger.forEach { beregnDager(it.tilSkalBeregnes()) }

                /** Meldeperiodene som skal beregnes erstatter evt. eksisterende beregninger på samme kjede */
                val nyeOgEksisterendeMeldeperioder = meldeperioderSomBeregnes
                    .plus(påfølgendeBeregninger.map { it.tilSkalBeregnes() })
                    .distinctBy { it.kjedeId }.sortedBy { it.fraOgMed }

                nyeOgEksisterendeMeldeperioder.mapNotNull { beregning ->
                    val nyeBeregnedeDager = beregnDager(beregning)

                    if (beregning.eksisterendeBeregning != null && nyeBeregnedeDager == beregning.eksisterendeBeregning) {
                        return@mapNotNull null
                    }

                    MeldeperiodeBeregning(
                        id = BeregningId.random(),
                        kjedeId = beregning.kjedeId,
                        beregningKilde = beregningKilde,
                        meldekortId = beregning.meldekortId,
                        dager = nyeBeregnedeDager,
                    )
                }
            }
    }

    private fun beregnDager(meldeperiode: MeldeperiodeSomSkalBeregnes): NonEmptyList<MeldeperiodeBeregningDag> {
        return meldeperiode.dager.map {
            beregnDag(
                meldeperiode.meldekortId,
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

/** Kan representere enten en tidligere beregnet meldeperiode, eller en ny meldeperiode
 *
 *  @param eksisterendeBeregning Forrige beregning for meldeperioden, eller null for ny meldeperiode
 * */
private data class MeldeperiodeSomSkalBeregnes(
    val kjedeId: MeldeperiodeKjedeId,
    val meldekortId: MeldekortId,
    val dager: NonEmptyList<MeldekortDag>,
    val eksisterendeBeregning: NonEmptyList<MeldeperiodeBeregningDag>?,
) {
    val fraOgMed: LocalDate = dager.first().dato
}

private fun MeldekortDager.tilSkalBeregnes(meldekortId: MeldekortId): MeldeperiodeSomSkalBeregnes {
    return MeldeperiodeSomSkalBeregnes(
        kjedeId = this.meldeperiode.kjedeId,
        meldekortId = meldekortId,
        dager = this.verdi.toNonEmptyListOrThrow(),
        eksisterendeBeregning = null,
    )
}

private fun MeldeperiodeBeregning.tilSkalBeregnes(): MeldeperiodeSomSkalBeregnes {
    return MeldeperiodeSomSkalBeregnes(
        kjedeId = this.kjedeId,
        meldekortId = this.meldekortId,
        dager = this.dager.map { dag ->
            MeldekortDag(
                dato = dag.dato,
                status = dag.tilMeldekortDagStatus(),
            )
        },
        eksisterendeBeregning = this.dager,
    )
}

fun Sak.beregnMeldekort(
    meldekortIdSomBeregnes: MeldekortId,
    meldeperiodeSomBeregnes: MeldekortDager,
): NonEmptyList<MeldeperiodeBeregning> {
    return beregnMeldekort(
        meldekortIdSomBeregnes = meldekortIdSomBeregnes,
        meldeperiodeSomBeregnes = meldeperiodeSomBeregnes,
        barnetilleggsPerioder = this.barnetilleggsperioder,
        tiltakstypePerioder = this.tiltakstypeperioder,
        meldeperiodeBeregninger = this.meldeperiodeBeregninger,
    )
}

fun Sak.beregnRevurderingStans(behandlingId: BehandlingId): BehandlingBeregning? {
    val behandling = hentBehandling(behandlingId)

    require(behandling is Revurdering && behandling.resultat is RevurderingResultat.Stans) {
        "Behandlingen på være en revurdering til stans"
    }

    val stansPeriode = behandling.virkningsperiode!!

    val beregningerSomSkalOppdateres = this.meldeperiodeBeregninger
        .sisteBeregningerForPeriode(stansPeriode)
        .map { beregning ->
            beregning.tilSkalBeregnes().copy(
                dager = beregning.dager.map { dag ->
                    MeldekortDag(
                        dato = dag.dato,
                        status = if (stansPeriode.contains(dag.dato)) {
                            MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
                        } else {
                            dag.tilMeldekortDagStatus()
                        },
                    )
                },
            )
        }.toNonEmptyListOrNull()

    if (beregningerSomSkalOppdateres == null) {
        return null
    }

    return BeregnMeldeperioder(
        beregningKilde = BeregningKilde.BeregningKildeBehandling(behandling.id),
        meldeperioderSomBeregnes = beregningerSomSkalOppdateres,
        barnetilleggsPerioder = this.barnetilleggsperioder,
        tiltakstypePerioder = this.tiltakstypeperioder,
        meldeperiodeBeregninger = this.meldeperiodeBeregninger,
    ).beregn().toNonEmptyListOrNull()?.let { BehandlingBeregning(it) }
}

fun beregnMeldekort(
    meldekortIdSomBeregnes: MeldekortId,
    meldeperiodeSomBeregnes: MeldekortDager,
    barnetilleggsPerioder: Periodisering<AntallBarn>,
    tiltakstypePerioder: Periodisering<TiltakstypeSomGirRett>,
    meldeperiodeBeregninger: MeldeperiodeBeregninger,
): NonEmptyList<MeldeperiodeBeregning> {
    return BeregnMeldeperioder(
        meldeperioderSomBeregnes = nonEmptyListOf(meldeperiodeSomBeregnes.tilSkalBeregnes(meldekortIdSomBeregnes)),
        barnetilleggsPerioder = barnetilleggsPerioder,
        tiltakstypePerioder = tiltakstypePerioder,
        beregningKilde = BeregningKilde.BeregningKildeMeldekort(meldekortIdSomBeregnes),
        meldeperiodeBeregninger = meldeperiodeBeregninger,
    ).beregn().toNonEmptyListOrThrow()
}
