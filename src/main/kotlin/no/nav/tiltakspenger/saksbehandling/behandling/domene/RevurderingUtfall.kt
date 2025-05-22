package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface RevurderingUtfall : BehandlingUtfall {
    data class Stans(
        val valgtHjemmel: List<ValgtHjemmelHarIkkeRettighet>,
    ) : RevurderingUtfall {

        init {
            // TODO: flytt denne sjekken lengre ut og bruk ValgtHjemmelForStans i parameter-typen
            require(valgtHjemmel.all { it is ValgtHjemmelForStans }) {
                "Ugyldig hjemmel for stans $valgtHjemmel"
            }
        }
    }
}
