package no.nav.tiltakspenger.saksbehandling.benk

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype

enum class BenkBehandlingstype {
    SØKNAD,
    SØKNADSBEHANDLING,
    REVURDERING,
}

fun Behandlingstype.toBenkBehandlingstype(): BenkBehandlingstype =
    when (this) {
        Behandlingstype.SØKNADSBEHANDLING -> BenkBehandlingstype.SØKNADSBEHANDLING
        Behandlingstype.REVURDERING -> BenkBehandlingstype.REVURDERING
    }
