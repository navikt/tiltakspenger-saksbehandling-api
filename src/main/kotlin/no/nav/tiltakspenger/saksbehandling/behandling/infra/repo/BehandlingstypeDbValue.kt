package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Behandlingstype

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
