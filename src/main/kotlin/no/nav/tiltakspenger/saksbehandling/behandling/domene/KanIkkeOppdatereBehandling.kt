package no.nav.tiltakspenger.saksbehandling.behandling.domene

sealed interface KanIkkeOppdatereBehandling {
    /** Inntil videre støtter vi ikke vedtak over allerede utbetalte perioder (gjelder søknadsbehandling) */
    data object InnvilgelsesperiodenOverlapperMedUtbetaltPeriode : KanIkkeOppdatereBehandling
    data class BehandlingenEiesAvAnnenSaksbehandler(val eiesAvSaksbehandler: String?) : KanIkkeOppdatereBehandling
    data object MåVæreUnderBehandling : KanIkkeOppdatereBehandling
    data object ErPaVent : KanIkkeOppdatereBehandling
}

sealed interface KanIkkeOppdatereOmgjøring : KanIkkeOppdatereBehandling {
    data object KanIkkeOpphøre : KanIkkeOppdatereOmgjøring
    data object KanIkkeOmgjøreFlereVedtak : KanIkkeOppdatereOmgjøring
    data object MåOmgjøreMinstEttVedtak : KanIkkeOppdatereOmgjøring
    data object MåOmgjøreAngittVedtak : KanIkkeOppdatereOmgjøring
    data object PerioderSomOmgjøresMåVæreSammenhengende : KanIkkeOppdatereOmgjøring
    data object OmgjøringsperiodeMåVæreGjeldende : KanIkkeOppdatereOmgjøring
    data object InnvilgelsesperioderOverlapperIkkeomgjortPeriode : KanIkkeOppdatereOmgjøring
}
