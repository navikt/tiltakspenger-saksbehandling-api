package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

data class Revurderinger(
    val revurderinger: List<Behandling>,
) : List<Behandling> by revurderinger {
    init {
        require(revurderinger.all { it.erRevurdering })
    }
}
