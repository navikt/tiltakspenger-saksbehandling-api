package no.nav.tiltakspenger.saksbehandling.klage.domene.forvaltningsmelding

import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeOppdatereKlagebehandling

sealed interface KanIkkeSendeForvaltningsmelding {
    data object ForvaltningsmeldingErAlleredeSendt : KanIkkeSendeForvaltningsmelding

    data object ManglerTekst : KanIkkeSendeForvaltningsmelding

    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String?,
        val faktiskSaksbehandler: String,
    ) : KanIkkeSendeForvaltningsmelding

    data class KanIkkeOppdateres(
        val underliggende: KanIkkeOppdatereKlagebehandling,
    ) : KanIkkeSendeForvaltningsmelding
}
