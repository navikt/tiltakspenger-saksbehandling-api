package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface Valideringsfeil {
    data class UtdøvendeSaksbehandlerErIkkePåBehandlingen(val eiesAv: String) : Valideringsfeil
    data object BehandlingenErIkkeUnderBehandling : Valideringsfeil
}
