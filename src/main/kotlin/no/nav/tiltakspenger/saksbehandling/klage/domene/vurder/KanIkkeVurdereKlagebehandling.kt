package no.nav.tiltakspenger.saksbehandling.klage.domene.vurder

sealed interface KanIkkeVurdereKlagebehandling {
    data object KanIkkeOppdateres : KanIkkeVurdereKlagebehandling

    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String,
        val faktiskSaksbehandler: String,
    ) : KanIkkeVurdereKlagebehandling
}
