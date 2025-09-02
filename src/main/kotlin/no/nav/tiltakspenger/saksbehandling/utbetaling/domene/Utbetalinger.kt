package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.leggSammen

data class Utbetalinger(
    val verdi: List<VedtattUtbetaling>,
) : List<VedtattUtbetaling> by verdi {

    /**
     * Kan inneholde hull, men ikke overlapp.
     */
    val perioder: List<Periode> by lazy {
        verdi.map { it.periode }.leggSammen(godtaOverlapp = true)
    }

    private val utbetalingerMap: Map<Ulid, VedtattUtbetaling> by lazy {
        verdi.associateBy { it.beregningKilde.id }
    }

    fun hentUtbetaling(id: BehandlingId): VedtattUtbetaling? {
        return utbetalingerMap[id]
    }

    fun leggTil(utbetaling: VedtattUtbetaling): Utbetalinger {
        return Utbetalinger((verdi + utbetaling).sortedBy { it.opprettet })
    }

    fun hentUtbetalingerFraPeriode(periode: Periode): List<VedtattUtbetaling> {
        return verdi.filter { periode.overlapperMed(it.periode) }
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
