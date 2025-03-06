package no.nav.tiltakspenger.saksbehandling.domene.behandling

sealed interface KanIkkeOppretteBehandling {
    data object IngenRelevanteTiltak : KanIkkeOppretteBehandling
}
