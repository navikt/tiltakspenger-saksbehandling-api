package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.benk.BenkBehandlingstype

enum class BehandlingstypeDTO {
    SØKNAD,
    FØRSTEGANGSBEHANDLING,
    REVURDERING,
}

fun BenkBehandlingstype.toBehandlingstypeDTO(): BehandlingstypeDTO =
    when (this) {
        BenkBehandlingstype.FØRSTEGANGSBEHANDLING -> BehandlingstypeDTO.FØRSTEGANGSBEHANDLING
        BenkBehandlingstype.REVURDERING -> BehandlingstypeDTO.REVURDERING
        BenkBehandlingstype.SØKNAD -> BehandlingstypeDTO.SØKNAD
    }
