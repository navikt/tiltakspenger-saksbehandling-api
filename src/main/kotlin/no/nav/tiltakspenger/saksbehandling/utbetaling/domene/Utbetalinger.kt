package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.leggSammen
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.toTidslinje

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

    private val utbetalingerMap: Map<Ulid, VedtattUtbetaling> by lazy {
        verdi.flatMap { utbetaling ->
            listOf(
                utbetaling.beregningKilde.id to utbetaling,
                utbetaling.id to utbetaling,
            )
        }.toMap()
    }

    fun hentUtbetalingForBehandlingId(id: BehandlingId): VedtattUtbetaling? {
        return utbetalingerMap[id]
    }

    fun hentUtbetaling(id: UtbetalingId): VedtattUtbetaling? {
        return utbetalingerMap[id]
    }

    fun hentUtbetalingForUuid(uuid: String): VedtattUtbetaling? {
        return verdi.find { it.id.uuidPart() == uuid }
    }

    fun leggTil(utbetaling: VedtattUtbetaling): Utbetalinger {
        return Utbetalinger((verdi + utbetaling).sortedBy { it.opprettet })
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
