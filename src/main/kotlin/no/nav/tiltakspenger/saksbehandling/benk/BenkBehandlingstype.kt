package no.nav.tiltakspenger.saksbehandling.benk

import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Behandlingstype

enum class BenkBehandlingstype {
    SØKNAD,
    FØRSTEGANGSBEHANDLING,
    REVURDERING,
}

fun Behandlingstype.toBenkBehandlingstype(): BenkBehandlingstype =
    when (this) {
        Behandlingstype.FØRSTEGANGSBEHANDLING -> BenkBehandlingstype.FØRSTEGANGSBEHANDLING
        Behandlingstype.REVURDERING -> BenkBehandlingstype.REVURDERING
    }
