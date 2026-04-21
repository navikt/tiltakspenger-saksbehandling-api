package no.nav.tiltakspenger.saksbehandling.klage.domene.opprettRammebehandlingFraKlage

import no.nav.tiltakspenger.libs.common.RammebehandlingId

sealed interface KanIkkeOppretteRammebehandlingFraKlage {
    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String,
        val faktiskSaksbehandler: String,
    ) : KanIkkeOppretteRammebehandlingFraKlage

    data class FinnesÅpenRammebehandling(val rammebehandlingId: RammebehandlingId) : KanIkkeOppretteRammebehandlingFraKlage
}
