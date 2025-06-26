package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface KanIkkeIverksetteBehandling {
    data class BehandlingenEiesAvAnnenBeslutter(val eiesAvBeslutter: String?) : KanIkkeIverksetteBehandling
}
