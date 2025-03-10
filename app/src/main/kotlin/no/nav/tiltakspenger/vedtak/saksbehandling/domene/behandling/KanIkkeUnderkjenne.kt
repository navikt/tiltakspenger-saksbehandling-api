package no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling

sealed interface KanIkkeUnderkjenne {
    data object MåVæreBeslutter : KanIkkeUnderkjenne
}
