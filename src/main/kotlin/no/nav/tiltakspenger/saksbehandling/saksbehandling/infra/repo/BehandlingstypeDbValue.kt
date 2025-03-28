package no.nav.tiltakspenger.saksbehandling.saksbehandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlingstype

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
