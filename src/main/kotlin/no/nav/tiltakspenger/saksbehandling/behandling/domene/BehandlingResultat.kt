package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface BehandlingResultat {
    val type: BehandlingResultatType get() = when (this) {
        is RevurderingResultat.Innvilgelse -> RevurderingType.INNVILGELSE
        is RevurderingResultat.Stans -> RevurderingType.STANS
        is SøknadsbehandlingResultat.Avslag -> SøknadsbehandlingType.AVSLAG
        is SøknadsbehandlingResultat.Innvilgelse -> SøknadsbehandlingType.INNVILGELSE
    }
}

sealed interface BehandlingResultatType

enum class SøknadsbehandlingType : BehandlingResultatType {
    INNVILGELSE,
    AVSLAG,
}

enum class RevurderingType : BehandlingResultatType {
    STANS,
    INNVILGELSE,
}
