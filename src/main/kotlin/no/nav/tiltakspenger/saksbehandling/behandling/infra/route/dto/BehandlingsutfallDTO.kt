package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsutfall

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

internal fun Behandlingsutfall.toBehandlingsutfallDto(): BehandlingsutfallDTO = when (this) {
    Behandlingsutfall.INNVILGELSE -> BehandlingsutfallDTO.INNVILGELSE
    Behandlingsutfall.AVSLAG -> BehandlingsutfallDTO.AVSLAG
    Behandlingsutfall.STANS -> BehandlingsutfallDTO.STANS
}
