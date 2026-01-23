package no.nav.tiltakspenger.saksbehandling.klage.domene.opprett

sealed interface KanIkkeOppretteRammebehandlingFraKlage {
    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String,
        val faktiskSaksbehandler: String,
    ) : KanIkkeOppretteRammebehandlingFraKlage
}
