package no.nav.tiltakspenger.vedtak.repository.behandling

import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.Behandlingstype

fun Behandlingstype.toDbValue(): String {
    return when (this) {
        Behandlingstype.FØRSTEGANGSBEHANDLING -> "FØRSTEGANGSBEHANDLING"
        Behandlingstype.REVURDERING -> "REVURDERING"
    }
}

fun String.toBehandlingstype(): Behandlingstype {
    return when (this) {
        "FØRSTEGANGSBEHANDLING" -> Behandlingstype.FØRSTEGANGSBEHANDLING
        "REVURDERING" -> Behandlingstype.REVURDERING
        else -> throw IllegalArgumentException("Ukjent behandlingstype: $this")
    }
}
