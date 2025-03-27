package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

sealed interface KanIkkeUnderkjenne {
    data object MåVæreBeslutter : KanIkkeUnderkjenne
    data object ManglerBegrunnelse : KanIkkeUnderkjenne
}
