package no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling

sealed interface KunneIkkeOppdatereBarnetillegg {
    data class KunneIkkeOppdatereBehandling(
        val valideringsfeil: no.nav.tiltakspenger.saksbehandling.behandling.domene.Valideringsfeil,
    ) : KunneIkkeOppdatereBarnetillegg
}
