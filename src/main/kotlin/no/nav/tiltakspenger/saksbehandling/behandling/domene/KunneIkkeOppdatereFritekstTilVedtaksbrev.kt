package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface KunneIkkeOppdatereFritekstTilVedtaksbrev {
    data class KunneIkkeOppdatereBehandling(
        val valideringsfeil: no.nav.tiltakspenger.saksbehandling.behandling.domene.Valideringsfeil,
    ) : KunneIkkeOppdatereFritekstTilVedtaksbrev
}
