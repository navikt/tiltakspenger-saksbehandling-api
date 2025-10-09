package no.nav.tiltakspenger.saksbehandling.beregning

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
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
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.IkkeBesvart
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.IkkeRettTilTiltakspenger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus.FRAVÆR_ANNET
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus.FRAVÆR_SYK
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus.FRAVÆR_SYKT_BARN
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus.IKKE_BESVART
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDagStatus.IKKE_TILTAKSDAG
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortDager
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.tilMeldekortDagStatus
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
    private val egenSykeperiode: SykedagerPeriode = SykedagerPeriode()
    private val barnSykeperiode: SykedagerPeriode = SykedagerPeriode()

    init {
        meldeperioderSomBeregnes.zipWithNext().forEach { (a, b) ->
            require(a.tilOgMed.plusDays(1) == b.fraOgMed) {
                "Meldeperioder som skal beregnes må være sammenhengede uten overlapp - fikk $meldeperioderSomBeregnes"
            }
        }
    }

    /** Returnerer nye eller endrede beregninger for meldeperioder. Returnerer tom liste dersom ingen beregninger endres */
    fun beregn(): List<MeldeperiodeBeregning> {
        val beregningsperiode = Periode(
            meldeperioderSomBeregnes.first().fraOgMed,
            meldeperioderSomBeregnes.last().tilOgMed,
        )

        return meldeperiodeBeregninger.gjeldendeBeregninger
            .partition { it.fraOgMed < beregningsperiode.fraOgMed }
            .let { (beregningerFørBeregningsperioden, beregningerUnderOgEtterBeregningsperioden) ->
                /** Kjør gjennom tidligere beregninger for å sette riktig state for sykedager før vi gjør nye beregninger */
                beregningerFørBeregningsperioden.forEach { beregnDager(it.tilSkalBeregnes()) }

                val beregningerForBeregningsperioden = meldeperioderSomBeregnes.map {
                    MeldeperiodeBeregning(
                        id = BeregningId.random(),
                        beregningKilde = beregningKilde,
                        kjedeId = it.kjedeId,
                        meldekortId = it.meldekortId,
                        dager = beregnDager(it),
                    )
                }

                val beregningerEtterBeregningsperioden = beregningerUnderOgEtterBeregningsperioden
                    .dropWhile { it.tilOgMed <= beregningsperiode.tilOgMed }
                    .map { it.tilSkalBeregnes() }

                val beregningerEtterBeregningsperiodenOmberegnet = beregningerEtterBeregningsperioden
                    .map { beregning ->
                        val nyeBeregnedeDager = beregnDager(beregning)

                        val harEndringer = nyeBeregnedeDager != beregning.eksisterendeBeregning

                        MeldeperiodeBeregning(
                            id = BeregningId.random(),
                            beregningKilde = beregningKilde,
                            kjedeId = beregning.kjedeId,
                            meldekortId = beregning.meldekortId,
                            dager = nyeBeregnedeDager,
                        ) to harEndringer
                    }
                    /** Beregninger etter beregningsperioden skal i utgangspunktet kun med dersom de har endringer,
                     *  men vi ønsker ikke "hull" i beregningene på en behandling, så vi dropper kun de etter siste
                     *  meldeperiode uten endring
                     * */
                    .dropLastWhile { (_, harEndringer) -> !harEndringer }
                    .map { it.first }

                beregningerForBeregningsperioden.plus(beregningerEtterBeregningsperiodenOmberegnet)
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
            DELTATT_UTEN_LØNN_I_TILTAKET -> DeltattUtenLønnITiltaket.create(
                dato = dato,
                tiltakstype = tiltakstype,
                antallBarn = antallBarn,
            )

            DELTATT_MED_LØNN_I_TILTAKET -> DeltattMedLønnITiltaket.create(
                dato = dato,
                tiltakstype = tiltakstype,
                antallBarn = antallBarn,
            )

            IKKE_TILTAKSDAG -> IkkeDeltatt.create(
                dato = dato,
                tiltakstype = tiltakstype,
                antallBarn = antallBarn,
            )

            FRAVÆR_SYK -> SykBruker.create(
                dato = dato,
                tiltakstype = tiltakstype,
                reduksjon = egenSykeperiode.oppdaterOgFinnReduksjon(dato),
                antallBarn = antallBarn,
            )

            FRAVÆR_SYKT_BARN -> SyktBarn.create(
                dato = dato,
                tiltakstype = tiltakstype,
                reduksjon = barnSykeperiode.oppdaterOgFinnReduksjon(dato),
                antallBarn = antallBarn,
            )

            FRAVÆR_GODKJENT_AV_NAV -> FraværGodkjentAvNav.create(
                dato = dato,
                tiltakstype = tiltakstype,
                antallBarn = antallBarn,
            )

            FRAVÆR_ANNET -> FraværAnnet.create(
                dato = dato,
                tiltakstype = tiltakstype,
                antallBarn = antallBarn,
            )

            IKKE_BESVART -> IkkeBesvart.create(
                dato = dato,
                tiltakstype = tiltakstype,
                antallBarn = antallBarn,
            )

            IKKE_RETT_TIL_TILTAKSPENGER -> IkkeRettTilTiltakspenger(dato = dato)
        }
    }
}

/**
 *  Vi utbetaler 100% de første 3 dagene med sykefravær, 75% de 13 neste, og 0% for de påfølgende dagene.
 *  Etter en periode på minst 16 dager uten nytt sykefravær nullstilles telleren.
 *
 *  Samme regler gjelder for sykt barn eller barnepasser
 *
 *  Se Rundskriv om tiltakspenger til § 10 – Reduksjon av ytelse på grunn av fravær
 *  https://lovdata.no/nav/rundskriv/r76-13-02
 *  https://confluence.adeo.no/spaces/POAO/pages/553634914/Tiltakspengeforskriften+%C2%A7+10+-+reduksjon+pga+frav%C3%A6r
 * */
private class SykedagerPeriode {
    private var sisteSykedag: LocalDate? = null
    private var antallSykedager: Int = 0

    fun oppdaterOgFinnReduksjon(nySykedag: LocalDate): ReduksjonAvYtelsePåGrunnAvFravær {
        if (erFørsteDagINyPeriode(nySykedag)) {
            antallSykedager = 1
        } else {
            antallSykedager++
        }

        sisteSykedag = nySykedag

        return when (antallSykedager) {
            in 1..ANTALL_EGENMELDINGSDAGER -> IngenReduksjon
            in (ANTALL_EGENMELDINGSDAGER + 1)..ANTALL_ARBEIDSGIVERPERIODEDAGER -> Reduksjon
            in (ANTALL_ARBEIDSGIVERPERIODEDAGER + 1)..Int.MAX_VALUE -> YtelsenFallerBort
            else -> throw IllegalStateException("Ugyldig antall sykedager: $antallSykedager")
        }
    }

    private fun erFørsteDagINyPeriode(nySykedag: LocalDate): Boolean {
        return sisteSykedag == null || ChronoUnit.DAYS.between(
            sisteSykedag,
            nySykedag,
        ) > ANTALL_ARBEIDSGIVERPERIODEDAGER
    }

    companion object {
        private const val ANTALL_EGENMELDINGSDAGER = 3
        private const val ANTALL_ARBEIDSGIVERPERIODEDAGER = 16
    }
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
    val tilOgMed: LocalDate = dager.last().dato
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

fun Sak.beregnRevurderingStans(behandlingId: BehandlingId, stansperiode: Periode): Beregning? {
    val behandling = hentBehandling(behandlingId)

    require(behandling is Revurdering && behandling.resultat is RevurderingResultat.Stans) {
        "Behandlingen på være en revurdering til stans"
    }

    val beregningerSomSkalOppdateres = this.meldeperiodeBeregninger
        .sisteBeregningerForPeriode(stansperiode)
        .map { beregning ->
            beregning.tilSkalBeregnes().copy(
                dager = beregning.dager.map { dag ->
                    MeldekortDag(
                        dato = dag.dato,
                        status = if (stansperiode.contains(dag.dato)) {
                            IKKE_RETT_TIL_TILTAKSPENGER
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
    ).beregn().toNonEmptyListOrNull()?.let { Beregning(it) }
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
