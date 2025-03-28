package no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling

sealed interface KanIkkeOppretteBehandling {
    data object IngenRelevanteTiltak : KanIkkeOppretteBehandling
}
