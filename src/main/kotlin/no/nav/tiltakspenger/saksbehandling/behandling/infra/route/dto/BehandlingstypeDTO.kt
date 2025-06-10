package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype

enum class BehandlingstypeDTO {
    SØKNAD,
    SØKNADSBEHANDLING,
    REVURDERING,
}

fun Behandlingstype.tilBehandlingstypeDTO(): BehandlingstypeDTO = when (this) {
    Behandlingstype.SØKNADSBEHANDLING -> BehandlingstypeDTO.SØKNADSBEHANDLING
    Behandlingstype.REVURDERING -> BehandlingstypeDTO.REVURDERING
}
