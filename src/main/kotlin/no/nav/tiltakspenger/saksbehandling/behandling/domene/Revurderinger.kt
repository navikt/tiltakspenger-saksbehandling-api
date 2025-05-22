package no.nav.tiltakspenger.saksbehandling.behandling.domene

data class Revurderinger(
    val revurderinger: List<Revurdering>,
) : List<Revurdering> by revurderinger {

    fun harÅpenRevurdering(): Boolean {
        return revurderinger.any { !it.erAvsluttet }
    }
}
