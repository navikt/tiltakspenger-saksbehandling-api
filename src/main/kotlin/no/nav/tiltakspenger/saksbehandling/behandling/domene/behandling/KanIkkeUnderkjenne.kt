package no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling

sealed interface KanIkkeUnderkjenne {
    data object MåVæreBeslutter : KanIkkeUnderkjenne
    data object ManglerBegrunnelse : KanIkkeUnderkjenne
}
