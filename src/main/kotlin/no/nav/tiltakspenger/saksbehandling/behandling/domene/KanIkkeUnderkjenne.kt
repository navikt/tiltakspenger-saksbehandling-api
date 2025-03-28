package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface KanIkkeUnderkjenne {
    data object MåVæreBeslutter : KanIkkeUnderkjenne
    data object ManglerBegrunnelse : KanIkkeUnderkjenne
}
