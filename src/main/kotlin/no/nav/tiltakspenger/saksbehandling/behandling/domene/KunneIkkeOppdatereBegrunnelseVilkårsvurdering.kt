package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface KunneIkkeOppdatereBegrunnelseVilkårsvurdering {
    data class KunneIkkeOppdatereBehandling(
        val valideringsfeil: no.nav.tiltakspenger.saksbehandling.behandling.domene.Valideringsfeil,
    ) : KunneIkkeOppdatereBegrunnelseVilkårsvurdering
}
