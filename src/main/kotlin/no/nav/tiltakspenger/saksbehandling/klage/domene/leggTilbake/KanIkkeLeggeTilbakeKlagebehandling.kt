package no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake

import no.nav.tiltakspenger.saksbehandling.behandling.domene.leggTilbake.KanIkkeLeggeTilbakeRammebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KanIkkeOppdatereKlagebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.leggTilbake.KanIkkeLeggeTilbakeMeldekortbehandling

sealed interface KanIkkeLeggeTilbakeKlagebehandling {
    data class SaksbehandlerMismatch(
        val forventetSaksbehandler: String,
        val faktiskSaksbehandler: String?,
    ) : KanIkkeLeggeTilbakeKlagebehandling

    data class KanIkkeOppdateres(
        val underliggende: KanIkkeOppdatereKlagebehandling,
    ) : KanIkkeLeggeTilbakeKlagebehandling

    /** Rammebehandlingen klagebehandlingen er knyttet til kunne ikke legges tilbake. */
    data class FeilVedRammebehandling(
        val underliggende: KanIkkeLeggeTilbakeRammebehandling,
    ) : KanIkkeLeggeTilbakeKlagebehandling

    /** Meldekortbehandlingen klagebehandlingen er knyttet til kunne ikke legges tilbake. */
    data class FeilVedMeldekortbehandling(
        val underliggende: KanIkkeLeggeTilbakeMeldekortbehandling,
    ) : KanIkkeLeggeTilbakeKlagebehandling
}
