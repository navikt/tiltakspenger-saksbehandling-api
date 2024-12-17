package no.nav.tiltakspenger.vedtak.routes.behandling

import no.nav.tiltakspenger.saksbehandling.domene.benk.BenkBehandlingstype

enum class BehandlingstypeDTO {
    SØKNAD,
    FØRSTEGANGSBEHANDLING,
    REVURDERING,
}

fun BenkBehandlingstype.toDTO(): BehandlingstypeDTO =
    when (this) {
        BenkBehandlingstype.FØRSTEGANGSBEHANDLING -> BehandlingstypeDTO.FØRSTEGANGSBEHANDLING
        BenkBehandlingstype.REVURDERING -> BehandlingstypeDTO.REVURDERING
        BenkBehandlingstype.SØKNAD -> BehandlingstypeDTO.SØKNAD
    }
