package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface RevurderingUtfall : BehandlingUtfall {
    data class Stans(
        val valgtHjemmelHarIkkeRettighet: List<ValgtHjemmelHarIkkeRettighet>,
    ) : RevurderingUtfall
}
