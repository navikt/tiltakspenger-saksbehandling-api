package no.nav.tiltakspenger.saksbehandling.klage.ports

import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling

interface KlagebehandlingFakeRepo {
    fun lagreKlagebehandling(klagebehandling: Klagebehandling, transactionContext: TransactionContext? = null)
}
