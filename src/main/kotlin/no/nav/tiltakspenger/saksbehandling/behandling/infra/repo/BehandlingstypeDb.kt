package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype

private enum class BehandlingstypeDb {
    SØKNADSBEHANDLING,
    REVURDERING,
}

fun Behandlingstype.toDbValue(): String {
    return when (this) {
        Behandlingstype.SØKNADSBEHANDLING -> BehandlingstypeDb.SØKNADSBEHANDLING
        Behandlingstype.REVURDERING -> BehandlingstypeDb.REVURDERING
    }.toString()
}

fun String.toBehandlingstype(): Behandlingstype {
    return when (BehandlingstypeDb.valueOf(this)) {
        BehandlingstypeDb.SØKNADSBEHANDLING -> Behandlingstype.SØKNADSBEHANDLING
        BehandlingstypeDb.REVURDERING -> Behandlingstype.REVURDERING
    }
}
