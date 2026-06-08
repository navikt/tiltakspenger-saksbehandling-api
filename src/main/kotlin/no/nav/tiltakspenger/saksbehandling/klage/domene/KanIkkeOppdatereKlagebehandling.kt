package no.nav.tiltakspenger.saksbehandling.klage.domene

import arrow.core.Nel
import no.nav.tiltakspenger.libs.common.BehandlingId

/**
 * Brukes på tvers av alle oppdateringsoperasjoner på klagebehandling for å representere feil som kan oppstå ved oppdatering av klagebehandling.
 */
sealed interface KanIkkeOppdatereKlagebehandling {
    data class FeilKlagebehandlingsstatus(
        val forventetStatus: Nel<Klagebehandlingsstatus>,
        val faktiskStatus: Klagebehandlingsstatus,
    ) : KanIkkeOppdatereKlagebehandling

    data class FeilTilknyttetBehandlingsstatus(
        val forventetStatus: Nel<TilknyttetBehandlingsstatus>,
        val faktiskStatus: TilknyttetBehandlingsstatus?,
    ) : KanIkkeOppdatereKlagebehandling

    data class FeilResultat(
        val forventetResultat: String,
        val faktiskResultat: String?,
    ) : KanIkkeOppdatereKlagebehandling

    data class KlageErKnyttetTilBehandling(val behandlingId: List<BehandlingId>) : KanIkkeOppdatereKlagebehandling
}
