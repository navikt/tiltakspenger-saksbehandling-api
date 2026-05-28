package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning

/**
 * Kobling mellom en [Meldeperiodebehandling] og dens tilhørende [MeldeperiodeBeregning].
 *
 * [meldeperiodeberegning] kan være `null` dersom beregningen ikke er gjort enda
 */
data class MeldeperiodebehandlingMedBeregning(
    val meldeperiodebehandling: Meldeperiodebehandling,
    val meldeperiodeberegning: MeldeperiodeBeregning?,
)
