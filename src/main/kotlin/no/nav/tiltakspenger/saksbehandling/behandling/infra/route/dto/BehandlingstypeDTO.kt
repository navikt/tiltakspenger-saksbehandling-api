package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype

enum class BehandlingstypeDTO {
    SØKNAD,
    FØRSTEGANGSBEHANDLING, // TODO: endre til SØKNADSBEHANDLING her og i frontend
    REVURDERING,
}

fun Behandlingstype.tilBehandlingstypeDTO(): BehandlingstypeDTO = when (this) {
    Behandlingstype.SØKNADSBEHANDLING -> BehandlingstypeDTO.FØRSTEGANGSBEHANDLING
    Behandlingstype.REVURDERING -> BehandlingstypeDTO.REVURDERING
}
