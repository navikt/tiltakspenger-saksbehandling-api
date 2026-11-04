package no.nav.tiltakspenger.saksbehandling.ytelser.domene

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktMetadata

/**
 * Ytelsene fra sokos-utbetaldata pakket sammen med request, tider og svaret for kallet.
 * [metadata] har samme format som utbetalingsoversikten, og svaret er trimmet til ytelsestype og ytelsesperiode slik kilden sendte dem.
 */
data class YtelserMedMetadata(
    val ytelser: List<Ytelse>,
    val metadata: UtbetalingsoversiktMetadata,
)
