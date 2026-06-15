package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus

sealed interface KunneIkkeOvertaMeldekortbehandling {
    data object BehandlingenKanIkkeVæreGodkjentEllerIkkeRett : KunneIkkeOvertaMeldekortbehandling

    data object BehandlingenErIkkeKnyttetTilEnSaksbehandlerForÅOverta : KunneIkkeOvertaMeldekortbehandling

    data object BehandlingenErIkkeKnyttetTilEnBeslutterForÅOverta : KunneIkkeOvertaMeldekortbehandling

    data object BehandlingenMåVæreUnderBeslutningForÅOverta : KunneIkkeOvertaMeldekortbehandling

    data object SaksbehandlerOgBeslutterKanIkkeVæreDenSamme : KunneIkkeOvertaMeldekortbehandling

    data object KanIkkeOvertaAutomatiskBehandling : KunneIkkeOvertaMeldekortbehandling

    data object MåVæreSaksbehandler : KunneIkkeOvertaMeldekortbehandling

    data object MåVæreBeslutter : KunneIkkeOvertaMeldekortbehandling

    data class UgyldigStatus(val status: MeldekortbehandlingStatus) : KunneIkkeOvertaMeldekortbehandling
}
