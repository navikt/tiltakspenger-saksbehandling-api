package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.benk.BenkBehandlingstype

enum class BehandlingstypeDTO {
    SØKNAD,
    FØRSTEGANGSBEHANDLING, // TODO: endre til SØKNADSBEHANDLING her og i frontend
    REVURDERING,
}

fun BenkBehandlingstype.toBehandlingstypeDTO(): BehandlingstypeDTO =
    when (this) {
        BenkBehandlingstype.SØKNADSBEHANDLING -> BehandlingstypeDTO.FØRSTEGANGSBEHANDLING
        BenkBehandlingstype.REVURDERING -> BehandlingstypeDTO.REVURDERING
        BenkBehandlingstype.SØKNAD -> BehandlingstypeDTO.SØKNAD
    }

fun Behandlingstype.tilBehandlingstypeDTO(): BehandlingstypeDTO = when (this) {
    Behandlingstype.SØKNADSBEHANDLING -> BehandlingstypeDTO.FØRSTEGANGSBEHANDLING
    Behandlingstype.REVURDERING -> BehandlingstypeDTO.REVURDERING
}
