package no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold

sealed interface FeilVedOversendelseTilKabal {
    data object UkjentFeil : FeilVedOversendelseTilKabal

    data class FeilMedResponse(val metadata: OversendtKlageTilKabalMetadata) : FeilVedOversendelseTilKabal {
        // Prøver ikke på nytt hvis vi fikk bad request fra kabal
        val kanPrøvesIgjen: Boolean = metadata.statusKode != 400
    }
}
