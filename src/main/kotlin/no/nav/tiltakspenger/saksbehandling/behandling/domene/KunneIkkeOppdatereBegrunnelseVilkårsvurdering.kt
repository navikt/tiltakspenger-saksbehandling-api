package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface KunneIkkeOppdatereBegrunnelseVilkårsvurdering {
    data class KunneIkkeOppdatereBehandling(
        val valideringsfeil: KanIkkeOppdatereBehandling,
    ) : KunneIkkeOppdatereBegrunnelseVilkårsvurdering
}
