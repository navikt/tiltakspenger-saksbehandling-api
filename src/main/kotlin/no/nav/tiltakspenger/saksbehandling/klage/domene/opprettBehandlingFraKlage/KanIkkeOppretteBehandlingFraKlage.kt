package no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage

import no.nav.tiltakspenger.libs.common.Ulid

sealed interface KanIkkeOppretteBehandlingFraKlage {
    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String,
        val faktiskSaksbehandler: String,
    ) : KanIkkeOppretteBehandlingFraKlage

    data class FinnesÅpenBehandling(val behandlingId: Ulid) : KanIkkeOppretteBehandlingFraKlage
}
