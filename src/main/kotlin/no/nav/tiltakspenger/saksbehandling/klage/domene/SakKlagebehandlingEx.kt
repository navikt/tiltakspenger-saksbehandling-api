package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.saksbehandling.sak.Sak

fun Sak.leggTilKlagebehandling(klagebehandling: Klagebehandling): Sak {
    return this.copy(klagebehandlinger = this.klagebehandlinger + klagebehandling)
}
