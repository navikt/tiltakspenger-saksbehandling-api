package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

sealed interface KanIkkeOppretteBehandling {
    data object IngenRelevanteTiltak : KanIkkeOppretteBehandling
}
