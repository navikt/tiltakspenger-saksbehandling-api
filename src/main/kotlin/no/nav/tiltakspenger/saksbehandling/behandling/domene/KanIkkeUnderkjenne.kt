package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface KanIkkeUnderkjenne {
    data object ManglerBegrunnelse : KanIkkeUnderkjenne
}
