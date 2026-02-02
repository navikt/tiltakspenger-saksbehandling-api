package no.nav.tiltakspenger.saksbehandling.klage.ports

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling

interface KlagebehandlingRepo {
    fun lagreKlagebehandling(klagebehandling: Klagebehandling, sessionContext: SessionContext? = null)
    fun hentForRammebehandlingId(rammebehandlingId: BehandlingId): Klagebehandling?
    fun taBehandling(klagebehandling: Klagebehandling, sessionContext: SessionContext?): Boolean
    fun overtaBehandling(
        klagebehandling: Klagebehandling,
        nåværendeSaksbehandler: String,
        sessionContext: SessionContext?,
    ): Boolean
}
