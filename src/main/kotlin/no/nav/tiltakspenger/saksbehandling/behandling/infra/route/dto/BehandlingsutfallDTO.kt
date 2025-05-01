package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsutfall

enum class BehandlingsutfallDTO {
    INNVILGELSE,
    AVSLAG,
}

internal fun Behandlingsutfall.toBehandlingsutfallDto(): BehandlingsutfallDTO = when (this) {
    Behandlingsutfall.INNVILGELSE -> BehandlingsutfallDTO.INNVILGELSE
    Behandlingsutfall.AVSLAG -> BehandlingsutfallDTO.AVSLAG
}
