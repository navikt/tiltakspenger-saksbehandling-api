package no.nav.tiltakspenger.saksbehandling.klage.domene

import arrow.core.Nel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus

sealed interface KanIkkeOppdatereKlagebehandling {
    data class FeilKlagebehandlingsstatus(
        val forventetStatus: Nel<Klagebehandlingsstatus>,
        val faktiskStatus: Klagebehandlingsstatus,
    ) : KanIkkeOppdatereKlagebehandling

    data class FeilRammebehandlingssstatus(
        val forventetStatus: Nel<Rammebehandlingsstatus>,
        val faktiskStatus: Rammebehandlingsstatus?,
    ) : KanIkkeOppdatereKlagebehandling

    data class FeilResultat(
        val forventetResultat: String,
        val faktiskResultat: String?,
    ) : KanIkkeOppdatereKlagebehandling
}
