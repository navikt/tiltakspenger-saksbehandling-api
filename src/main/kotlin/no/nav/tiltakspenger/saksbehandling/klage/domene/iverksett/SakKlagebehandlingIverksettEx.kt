package no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilKlagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

/**
 * Reservert for iverksetting av avviste klager.
 * For medhold/omgjøring, se [no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettRammebehandlingService]
 */
fun Sak.iverksettKlagebehandling(
    kommando: IverksettKlagebehandlingKommando,
    clock: Clock,
): Either<KanIkkeIverksetteKlagebehandling, Pair<Sak, Klagevedtak>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).iverksett(kommando = kommando).map {
        val klagevedtak = Klagevedtak.createFromKlagebehandling(
            clock = clock,
            klagebehandling = it,
        )
        val oppdatertSak = this.oppdaterKlagebehandling(it).leggTilKlagevedtak(klagevedtak)
        Pair(oppdatertSak, klagevedtak)
    }
}
