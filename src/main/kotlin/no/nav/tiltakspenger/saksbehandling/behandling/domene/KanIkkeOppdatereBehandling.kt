package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.NonEmptySet

sealed interface KanIkkeOppdatereBehandling {
    /** Inntil videre støtter vi ikke vedtak over allerede utbetalte perioder (gjelder søknadsbehandling) */
    data object InnvilgelsesperiodenOverlapperMedUtbetaltPeriode : KanIkkeOppdatereBehandling
    data class BehandlingenEiesAvAnnenSaksbehandler(val eiesAvSaksbehandler: String?) : KanIkkeOppdatereBehandling
    data object MåVæreUnderBehandling : KanIkkeOppdatereBehandling
    data object ErPaVent : KanIkkeOppdatereBehandling
    data object KanIkkeOpphøre : KanIkkeOppdatereBehandling
}

sealed interface KanIkkeOppdatereOmgjøring : KanIkkeOppdatereBehandling {
    data object KanIkkeOmgjøreFlereVedtak : KanIkkeOppdatereOmgjøring
    data object MåOmgjøreMinstEttVedtak : KanIkkeOppdatereOmgjøring
    data object MåOmgjøreAngittVedtak : KanIkkeOppdatereOmgjøring
    data object MåOmgjøreEnSammenhengendePeriode : KanIkkeOppdatereOmgjøring
    data object VedtaksperiodeMåInneholdeInnvilgelsesperiodene : KanIkkeOppdatereOmgjøring
    data object KanIkkeOpphøreVedtakUtenGjeldendeInnvilgelse : KanIkkeOppdatereOmgjøring
    data class UgyldigPeriodeForOpphør(val årsak: String) : KanIkkeOppdatereOmgjøring
    data class MåHaFritekstForValgteHjemler(val hjemler: NonEmptySet<HjemmelForStansEllerOpphør>) : KanIkkeOppdatereOmgjøring
}
