package no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage

import no.nav.tiltakspenger.libs.common.BehandlingId

sealed interface KanIkkeOppretteBehandlingFraKlage {
    data object BehandlingenErSattPåVent : KanIkkeOppretteBehandlingFraKlage

    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String,
        val faktiskSaksbehandler: String,
    ) : KanIkkeOppretteBehandlingFraKlage

    data class FinnesÅpenBehandling(val behandlingId: BehandlingId) : KanIkkeOppretteBehandlingFraKlage

    data class KanIkkeOppretteMeldekortbehandling(
        val underliggende: no.nav.tiltakspenger.saksbehandling.meldekort.service.KanIkkeOppretteMeldekortbehandling,
    ) : KanIkkeOppretteBehandlingFraKlage
}
