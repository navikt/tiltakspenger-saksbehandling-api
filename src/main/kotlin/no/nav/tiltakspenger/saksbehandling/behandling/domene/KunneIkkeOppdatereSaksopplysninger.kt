package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface KunneIkkeOppdatereSaksopplysninger {
    data class KunneIkkeOppdatereBehandling(
        val valideringsfeil: no.nav.tiltakspenger.saksbehandling.behandling.domene.Valideringsfeil,
    ) : KunneIkkeOppdatereSaksopplysninger
}
