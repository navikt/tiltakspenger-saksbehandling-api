package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsutfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingUtfall

enum class BehandlingsutfallDTO {
    INNVILGELSE,
    AVSLAG,
    STANS,
    ;

    fun toDomain(): Behandlingsutfall = when (this) {
        INNVILGELSE -> Behandlingsutfall.INNVILGELSE
        AVSLAG -> Behandlingsutfall.AVSLAG
        STANS -> Behandlingsutfall.STANS
    }
}

fun Behandlingsutfall.toBehandlingsutfallDto(): BehandlingsutfallDTO = when (this) {
    Behandlingsutfall.INNVILGELSE -> BehandlingsutfallDTO.INNVILGELSE
    Behandlingsutfall.AVSLAG -> BehandlingsutfallDTO.AVSLAG
    Behandlingsutfall.STANS -> BehandlingsutfallDTO.STANS
}

fun SøknadsbehandlingUtfall.toBehandlingsutfallDto(): BehandlingsutfallDTO = when (this) {
    is SøknadsbehandlingUtfall.Avslag -> BehandlingsutfallDTO.AVSLAG
    is SøknadsbehandlingUtfall.Innvilgelse -> BehandlingsutfallDTO.INNVILGELSE
}
