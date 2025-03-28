package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface KanIkkeOppretteBehandling {
    data object IngenRelevanteTiltak : KanIkkeOppretteBehandling
}
