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

    /** Returnerer nye eller endrede beregninger for meldeperioder. Returnerer tom liste dersom ingen beregninger endres */
    fun beregn(): List<MeldeperiodeBeregning> {
        val meldeperioderSomBeregnesFraOgMed = meldeperioderSomBeregnes.first().fraOgMed

        return meldeperiodeBeregninger.gjeldendeBeregninger
            .partition { it.fraOgMed < meldeperioderSomBeregnesFraOgMed }
            .let { (tidligereBeregninger, påfølgendeBeregninger) ->
                /** Kjør gjennom tidligere beregninger for å sette riktig state for sykedager før vi gjør nye beregninger */
                tidligereBeregninger.forEach { beregnDager(it.tilSkalBeregnes()) }

                /** Meldeperiodene som skal beregnes erstatter evt. eksisterende beregninger på samme kjede */
                val nyeOgEksisterendeMeldeperioder: List<MeldeperiodeSomSkalBeregnes> = meldeperioderSomBeregnes
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
            MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> DeltattUtenLønnITiltaket.create(
                dato = dato,
                tiltakstype = tiltakstype,
                antallBarn = antallBarn,
            )

            MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> DeltattMedLønnITiltaket.create(
                dato = dato,
                tiltakstype = tiltakstype,
                antallBarn = antallBarn,
            )

            MeldekortDagStatus.IKKE_TILTAKSDAG -> IkkeDeltatt.create(
                dato = dato,
                tiltakstype = tiltakstype,
                antallBarn = antallBarn,
            )

            MeldekortDagStatus.FRAVÆR_SYK -> SykBruker.create(
                dato = dato,
                tiltakstype = tiltakstype,
                reduksjon = egenSykeperiode.oppdaterOgFinnReduksjon(dato),
                antallBarn = antallBarn,
            )

            MeldekortDagStatus.FRAVÆR_SYKT_BARN -> SyktBarn.create(
                dag = dato,
                tiltakstype = tiltakstype,
                reduksjon = barnSykeperiode.oppdaterOgFinnReduksjon(dato),
                antallBarn = antallBarn,
            )

            MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> FraværGodkjentAvNav.create(
                dato = dato,
                tiltakstype = tiltakstype,
                antallBarn = antallBarn,
            )

            MeldekortDagStatus.FRAVÆR_ANNET -> FraværAnnet.create(
                dato = dato,
                tiltakstype = tiltakstype,
                antallBarn = antallBarn,
            )

            MeldekortDagStatus.IKKE_BESVART -> IkkeBesvart.create(
                dato = dato,
                tiltakstype = tiltakstype,
                antallBarn = antallBarn,
            )

            MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> IkkeRettTilTiltakspenger(dato = dato)
        }
    }
}

/** Vi utbetaler 100% de første 3 dagene med sykefravær, og 75% de 13 neste.
 *  Etter denne 16-dagers perioden må det gå minst 16 dager uten nytt sykefravær
 *  før telleren resettes og vi igjen utbetaler for sykedager.
 *
 *  Samme regler gjelder for sykt barn eller barnepasser
 *
 *  Se Rundskriv om tiltakspenger til § 10 – Reduksjon av ytelse på grunn av fravær
 *  https://lovdata.no/nav/rundskriv/r76-13-02
 * */
private class SykedagerPeriode {
    private var startDag: LocalDate? = null
    private var sisteSykedag: LocalDate? = null
    private var antallSykedager: Int = 0

    fun oppdaterOgFinnReduksjon(nySykedag: LocalDate): ReduksjonAvYtelsePåGrunnAvFravær {
        if (startDag == null) {
            reset(nySykedag)
            return IngenReduksjon
        }

        val dagerSidenForrigeSykedag = ChronoUnit.DAYS.between(sisteSykedag, nySykedag)

        if (dagerSidenForrigeSykedag > DAGER_KARANTENE) {
            reset(nySykedag)
            return IngenReduksjon
        }

        sisteSykedag = nySykedag
        antallSykedager++

        return when (antallSykedager) {
            in 1..ANTALL_EGENMELDINGSDAGER -> IngenReduksjon
            in 4..DAGER_KARANTENE -> Reduksjon
            in DAGER_KARANTENE + 1..Int.MAX_VALUE -> YtelsenFallerBort
            else -> throw IllegalStateException("Ugyldig antall sykedager: $antallSykedager")
        }
    }

    private fun reset(dag: LocalDate) {
        startDag = dag
        sisteSykedag = dag
        antallSykedager = 1
    }

    companion object {
        private const val ANTALL_EGENMELDINGSDAGER = 3
        private const val DAGER_KARANTENE = 16
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
