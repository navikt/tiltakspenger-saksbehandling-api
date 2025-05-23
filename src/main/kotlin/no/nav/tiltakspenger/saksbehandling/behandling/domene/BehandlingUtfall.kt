package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface BehandlingUtfall {
    val type: BehandlingUtfallType get() = when (this) {
        is RevurderingUtfall.Innvilgelse -> RevurderingUtfallType.INNVILGELSE
        is RevurderingUtfall.Stans -> RevurderingUtfallType.STANS
        is SøknadsbehandlingUtfall.Avslag -> SøknadsbehandlingUtfallType.AVSLAG
        is SøknadsbehandlingUtfall.Innvilgelse -> SøknadsbehandlingUtfallType.INNVILGELSE
    }
}

sealed interface BehandlingUtfallType

enum class SøknadsbehandlingUtfallType : BehandlingUtfallType {
    INNVILGELSE,
    AVSLAG,
}

enum class RevurderingUtfallType : BehandlingUtfallType {
    STANS,
    INNVILGELSE,
}
