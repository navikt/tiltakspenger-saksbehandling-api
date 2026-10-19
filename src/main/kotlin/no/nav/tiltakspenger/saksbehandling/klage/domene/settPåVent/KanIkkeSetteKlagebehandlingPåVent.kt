package no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent

import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeOppdatereKlagebehandling

sealed interface KanIkkeSetteKlagebehandlingPåVent {
    data object BehandlingenErIkkeLengerUnderBeslutning : KanIkkeSetteKlagebehandlingPåVent

    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String,
        val faktiskSaksbehandler: String?,
    ) : KanIkkeSetteKlagebehandlingPåVent

    data class KanIkkeOppdateres(
        val underliggende: KanIkkeOppdatereKlagebehandling,
    ) : KanIkkeSetteKlagebehandlingPåVent

    data object KlagenMåVæreSattPåVentForÅKunneOppdateres : KanIkkeSetteKlagebehandlingPåVent

    /**
     * Den tilknyttede ramme- eller meldekortbehandlingen venter hos beslutter, og utøvende bruker mangler beslutterrolle.
     * Klagen følger den tilknyttede behandlingen, så det er den behandlingens tilgangsregler som gjelder.
     */
    data object MåVæreBeslutterForÅKunneSetteKlagenPåVent : KanIkkeSetteKlagebehandlingPåVent
}
