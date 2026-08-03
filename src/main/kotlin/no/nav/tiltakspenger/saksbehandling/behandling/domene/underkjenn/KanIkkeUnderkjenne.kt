package no.nav.tiltakspenger.saksbehandling.behandling.domene.underkjenn

sealed interface KanIkkeUnderkjenne {
    data object ManglerBegrunnelse : KanIkkeUnderkjenne
}
