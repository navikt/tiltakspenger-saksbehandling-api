package no.nav.tiltakspenger.saksbehandling.klage.domene.brev

sealed interface KanIkkeOppdatereBrevtekstPåKlagebehandling {
    /** Behandlingen er i en tilstand der den ikke kan oppdateres. */
    data object KanIkkeOppdateres : KanIkkeOppdatereBrevtekstPåKlagebehandling
    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String,
        val faktiskSaksbehandler: String,
    ) : KanIkkeOppdatereBrevtekstPåKlagebehandling
}
