package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype

enum class RammebehandlingstypeDTO {
    SØKNAD,
    SØKNADSBEHANDLING,
    REVURDERING,
}

fun Behandlingstype.tilBehandlingstypeDTO(): RammebehandlingstypeDTO = when (this) {
    Behandlingstype.SØKNADSBEHANDLING -> RammebehandlingstypeDTO.SØKNADSBEHANDLING
    Behandlingstype.REVURDERING -> RammebehandlingstypeDTO.REVURDERING
}
