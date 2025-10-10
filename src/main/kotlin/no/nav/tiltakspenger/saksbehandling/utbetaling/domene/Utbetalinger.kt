package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.leggSammen
import no.nav.tiltakspenger.libs.periodisering.toTidslinje
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag
import no.nav.utsjekk.kontrakter.felles.Satstype
import no.nav.utsjekk.kontrakter.iverksett.StønadsdataTiltakspengerV2Dto
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Inneholder alle utbetalinger (som er en konsekvens av et vedtak).
 * De vil stamme både fra meldekortvedtak, søknadsbehandlingsvedtak og revurderingsvedtak.
 * Merk at de er sortert på opprettet dato.
 * En nyere utbetaling kan overlappe en tidligere og vil da erstatte den overlappende delen.
 */
data class Utbetalinger(
    val verdi: List<VedtattUtbetaling>,
) : List<VedtattUtbetaling> by verdi {

    /**
     * Kan inneholde hull, men ikke overlapp.
     */
    val perioder: List<Periode> by lazy {
        verdi.map { it.periode }.leggSammen(godtaOverlapp = true)
    }

    val tidslinje: Periodisering<VedtattUtbetaling> by lazy {
        verdi.toTidslinje()
    }

    private val satstypeTidslinje: Periodisering<Satstype> by lazy {
        tidslinje
            .mapNotNull { it.verdi.sendtTilUtbetaling?.requestDto?.vedtak?.utbetalinger }
            .flatten()
            .mapNotNull {
                if ((it.stønadsdata as StønadsdataTiltakspengerV2Dto).barnetillegg) {
                    return@mapNotNull null
                }

                PeriodeMedVerdi(
                    verdi = it.satstype,
                    periode = Periode(it.fraOgMedDato, it.tilOgMedDato),
                )
            }
            .let { Periodisering(it) }
    }

    fun harUtbetalingIPeriode(periode: Periode): Boolean {
        return perioder.any { it.overlapperMed(periode) }
    }

    private val utbetalingerMap: Map<Ulid, VedtattUtbetaling> by lazy {
        verdi.associateBy { it.beregningKilde.id }
    }

    fun hentUtbetalingForBehandlingId(id: BehandlingId): VedtattUtbetaling? {
        return utbetalingerMap[id]
    }

    fun leggTil(utbetaling: VedtattUtbetaling): Utbetalinger {
        return Utbetalinger((verdi + utbetaling).sortedBy { it.opprettet })
    }

    fun hentUtbetalingerFraPeriode(periode: Periode): List<VedtattUtbetaling> {
        return verdi.filter { periode.overlapperMed(it.periode) }
    }

    fun hentSisteUtbetalingForDato(dato: LocalDate): VedtattUtbetaling? {
        return tidslinje.hentVerdiForDag(dato)
    }

    fun hentSisteBeregningdagForDato(dato: LocalDate): MeldeperiodeBeregningDag? {
        return hentSisteUtbetalingForDato(dato)?.hentBeregningsdagForDato(dato)
    }

    fun harDag7IMånedForDato(dato: LocalDate): Boolean {
        return harDag7IMånederForPeriode(
            Periode(
                dato.with(TemporalAdjusters.firstDayOfMonth()),
                dato.with(
                    TemporalAdjusters.lastDayOfMonth(),
                ),
            ),
        )
    }

    fun harDag7IMånederForPeriode(periode: Periode): Boolean {
        val periodeForHeleMåneder = Periode(
            periode.fraOgMed.with(TemporalAdjusters.firstDayOfMonth()),
            periode.tilOgMed.with(TemporalAdjusters.lastDayOfMonth()),
        )

        return harDag7IPeriode(periodeForHeleMåneder)
    }

    private fun harDag7IPeriode(periode: Periode): Boolean {
        return satstypeTidslinje.krymp(periode).perioderMedVerdi.any {
            it.verdi == Satstype.DAGLIG_INKL_HELG
        }
    }

    init {
        if (verdi.isNotEmpty()) {
            require(
                verdi.zipWithNext()
                    .all { (a, b) -> a.id == b.forrigeUtbetalingId },
            ) { "Utbetalingene må lenke til forrige utbetaling, men var ${verdi.map { it.id to it.forrigeUtbetalingId }}" }

            require(
                verdi.zipWithNext()
                    .all { (a, b) -> a.opprettet < b.opprettet },
            ) { "Utbetalingene må være sortert på opprettet dato" }

            require(verdi.first().forrigeUtbetalingId == null) {
                "Første 'forrigeUtbetalingId' må være null, men var ${verdi.first().forrigeUtbetalingId}"
            }
        }
    }
}
