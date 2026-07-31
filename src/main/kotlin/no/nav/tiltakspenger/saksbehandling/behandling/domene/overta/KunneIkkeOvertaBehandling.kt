package no.nav.tiltakspenger.saksbehandling.behandling.domene.overta

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle

sealed interface KunneIkkeOvertaBehandling {
    /** Utøvende bruker mangler saksbehandlerrolle. */
    data object MåVæreSaksbehandler : KunneIkkeOvertaBehandling

    /** Utøvende bruker mangler beslutterrolle. */
    data object MåVæreBeslutter : KunneIkkeOvertaBehandling

    data object BehandlingenKanIkkeVæreVedtattEllerAvbrutt : KunneIkkeOvertaBehandling

    data object BehandlingenKanIkkeVæreUnderAutomatiskBehandling : KunneIkkeOvertaBehandling

    data object BehandlingenErIkkeKnyttetTilEnSaksbehandlerForÅOverta : KunneIkkeOvertaBehandling

    data object BehandlingenMåVæreUnderBehandlingForÅOverta : KunneIkkeOvertaBehandling

    data object BehandlingenMåVæreUnderBeslutningForÅOverta : KunneIkkeOvertaBehandling

    data object BehandlingenErIkkeKnyttetTilEnBeslutterForÅOverta : KunneIkkeOvertaBehandling

    data object SaksbehandlerOgBeslutterKanIkkeVæreDenSamme : KunneIkkeOvertaBehandling

    data object BehandlingenErUnderAktivBehandling : KunneIkkeOvertaBehandling

    data class KanIkkeOvertaKlagebehandling(val underliggende: no.nav.tiltakspenger.saksbehandling.klage.domene.overta.KanIkkeOvertaKlagebehandling) : KunneIkkeOvertaBehandling
}

/**
 * Kaster [no.nav.tiltakspenger.saksbehandling.felles.TilgangException] dersom feilen skyldes at [saksbehandler] mangler en rolle.
 * Manglende rolle er en tilgangsfeil (403), ikke en tilstandsfeil, og skal derfor ikke returneres som en venstre-verdi.
 */
fun KunneIkkeOvertaBehandling.kastVedManglendeRolle(saksbehandler: Saksbehandler) {
    when (this) {
        KunneIkkeOvertaBehandling.MåVæreSaksbehandler -> krevSaksbehandlerRolle(saksbehandler)
        KunneIkkeOvertaBehandling.MåVæreBeslutter -> krevBeslutterRolle(saksbehandler)
        else -> Unit
    }
}
