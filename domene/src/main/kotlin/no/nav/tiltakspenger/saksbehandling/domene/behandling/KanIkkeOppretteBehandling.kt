package no.nav.tiltakspenger.saksbehandling.domene.behandling

sealed interface KanIkkeOppretteBehandling {
    data object FantIkkeTiltak : KanIkkeOppretteBehandling
    data object StøtterKunInnvilgelse : KanIkkeOppretteBehandling
}
