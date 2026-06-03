package no.nav.tiltakspenger.saksbehandling.klage.domene.opprettRammebehandlingFraKlage

import no.nav.tiltakspenger.libs.common.BehandlingId

sealed interface KanIkkeOppretteBehandlingFraKlage {
    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String,
        val faktiskSaksbehandler: String,
    ) : KanIkkeOppretteBehandlingFraKlage

    data class FinnesÅpenBehandling(val behandlingId: BehandlingId) : KanIkkeOppretteBehandlingFraKlage
}
