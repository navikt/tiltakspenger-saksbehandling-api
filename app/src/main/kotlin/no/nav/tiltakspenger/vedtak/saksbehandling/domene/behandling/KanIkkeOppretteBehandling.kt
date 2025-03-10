package no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling

sealed interface KanIkkeOppretteBehandling {
    data object IngenRelevanteTiltak : KanIkkeOppretteBehandling
}
