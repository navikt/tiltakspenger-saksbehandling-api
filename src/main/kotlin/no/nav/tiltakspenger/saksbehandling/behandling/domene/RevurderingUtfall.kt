package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface RevurderingUtfall : BehandlingUtfall {
    data class Stans(
        val valgtHjemmel: List<ValgtHjemmelForStans>,
    ) : RevurderingUtfall

    data object Innvilgelse : RevurderingUtfall
}
