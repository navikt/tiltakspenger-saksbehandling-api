package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.periodisering.Periode

data class Utbetalinger(
    val verdi: List<Utbetaling>,
) : List<Utbetaling> by verdi {

    private val utbetalingerMap: Map<Ulid, Utbetaling> by lazy {
        verdi.associateBy { it.beregningKilde.id }
    }

    fun hentUtbetaling(id: MeldekortId): Utbetaling? {
        return utbetalingerMap[id]
    }

    fun hentUtbetaling(id: BehandlingId): Utbetaling? {
        return utbetalingerMap[id]
    }

    fun leggTil(utbetaling: Utbetaling): Utbetalinger {
        return Utbetalinger(verdi + utbetaling)
    }

    fun hentUtbetalingerFraPeriode(periode: Periode): List<Utbetaling> {
        return verdi.filter { periode.overlapperMed(it.periode) }
    }

    init {
        if (verdi.isNotEmpty()) {
            require(
                verdi.zipWithNext()
                    .all { (a, b) -> a.id == b.forrigeUtbetalingId },
            ) { "Utbetalingene må lenke til forrige utbetaling, men var ${verdi.map { it.vedtakId to it.forrigeUtbetalingId }}" }

            require(verdi.first().forrigeUtbetalingId == null) {
                "Første 'forrigeUtbetalingId' må være null, men var ${verdi.first().forrigeUtbetalingId}"
            }
        }
    }
}
