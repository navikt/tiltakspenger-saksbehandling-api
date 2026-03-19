package no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilKlagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.klagebehandling.genererSaksstatistikk
import java.time.Clock

/**
 * Reservert for iverksetting av avviste klager.
 * For medhold/omgjøring, se [no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.IverksettRammebehandlingService]
 */
fun Sak.iverksettAvvistKlagebehandling(
    kommando: IverksettAvvisningKommando,
    clock: Clock,
): Either<KanIkkeIverksetteKlagebehandling, Triple<Sak, Klagevedtak, Statistikkhendelser>> {
    return this
        .hentKlagebehandling(kommando.klagebehandlingId)
        .iverksettAvvisning(kommando = kommando)
        .map { oppdatertKlagebehandling ->
            val klagevedtak = Klagevedtak.createFromKlagebehandling(
                clock = clock,
                klagebehandling = oppdatertKlagebehandling,
            )
            val klagestatistikk = Statistikkhendelser(klagevedtak.genererSaksstatistikk())
            val oppdatertSak = this.oppdaterKlagebehandling(oppdatertKlagebehandling).leggTilKlagevedtak(klagevedtak)
            Triple(oppdatertSak, klagevedtak, klagestatistikk)
        }
}
