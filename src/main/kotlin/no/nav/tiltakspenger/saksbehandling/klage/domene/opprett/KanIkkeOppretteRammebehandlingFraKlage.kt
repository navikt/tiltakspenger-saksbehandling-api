package no.nav.tiltakspenger.saksbehandling.klage.domene.opprett

import no.nav.tiltakspenger.libs.common.BehandlingId

sealed interface KanIkkeOppretteRammebehandlingFraKlage {
    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String,
        val faktiskSaksbehandler: String,
    ) : KanIkkeOppretteRammebehandlingFraKlage

    data class FinnesÅpenRammebehandling(val rammebehandlingId: BehandlingId) : KanIkkeOppretteRammebehandlingFraKlage
}
