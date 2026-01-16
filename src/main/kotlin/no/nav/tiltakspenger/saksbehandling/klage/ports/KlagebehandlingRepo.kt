package no.nav.tiltakspenger.saksbehandling.klage.ports

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling

interface KlagebehandlingRepo {
    fun lagreKlagebehandling(klagebehandling: Klagebehandling, sessionContext: SessionContext? = null)
}
