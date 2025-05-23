package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface RevurderingResultat : BehandlingResultat {
    data class Stans(
        val valgtHjemmel: List<ValgtHjemmelForStans>,
    ) : RevurderingResultat

    data object Innvilgelse : RevurderingResultat
}
