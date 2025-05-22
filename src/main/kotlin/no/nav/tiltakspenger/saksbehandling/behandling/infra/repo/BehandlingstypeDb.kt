package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype

private enum class BehandlingstypeDb {
    FØRSTEGANGSBEHANDLING, // TODO: endre og migrer til SØKNADSBEHANDLING i db
    REVURDERING,
}

fun Behandlingstype.toDbValue(): String {
    return when (this) {
        Behandlingstype.SØKNADSBEHANDLING -> BehandlingstypeDb.FØRSTEGANGSBEHANDLING
        Behandlingstype.REVURDERING -> BehandlingstypeDb.REVURDERING
    }.toString()
}

fun String.toBehandlingstype(): Behandlingstype {
    return when (BehandlingstypeDb.valueOf(this)) {
        BehandlingstypeDb.FØRSTEGANGSBEHANDLING -> Behandlingstype.SØKNADSBEHANDLING
        BehandlingstypeDb.REVURDERING -> Behandlingstype.REVURDERING
    }
}
