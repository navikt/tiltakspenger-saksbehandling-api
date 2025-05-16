package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype

fun Behandlingstype.toDbValue(): String {
    return when (this) {
        Behandlingstype.SØKNADSBEHANDLING -> "FØRSTEGANGSBEHANDLING"
        Behandlingstype.REVURDERING -> "REVURDERING"
    }
}

fun String.toBehandlingstype(): Behandlingstype {
    return when (this) {
        "FØRSTEGANGSBEHANDLING" -> Behandlingstype.SØKNADSBEHANDLING
        "REVURDERING" -> Behandlingstype.REVURDERING
        else -> throw IllegalArgumentException("Ukjent behandlingstype: $this")
    }
}
