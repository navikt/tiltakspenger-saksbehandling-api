package no.nav.tiltakspenger.saksbehandling.klage.domene

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock
import java.time.LocalDateTime

fun Sak.leggTilKlagebehandling(klagebehandling: Klagebehandling): Sak {
    return this.copy(behandlinger = this.behandlinger.leggTilKlagebehandling(klagebehandling))
}

fun Sak.oppdaterKlagebehandling(klagebehandling: Klagebehandling): Sak {
    return this.copy(behandlinger = this.behandlinger.oppdaterKlagebehandling(klagebehandling))
}

fun Sak.hentKlagebehandling(klagebehandlingId: KlagebehandlingId): Klagebehandling {
    return this.behandlinger.hentKlagebehandling(klagebehandlingId)
}

fun Sak.oppdaterKlagebehandlingFormkrav(
    kommando: OppdaterKlagebehandlingFormkravKommando,
    journalpostOpprettet: LocalDateTime,
    clock: Clock,
): Either<KanIkkeOppdatereKlagebehandlingFormkrav, Pair<Sak, Klagebehandling>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId)
        .oppdaterFormkrav(kommando, journalpostOpprettet, clock)
        .map {
            val oppdatertSak = this.oppdaterKlagebehandling(it)
            Pair(oppdatertSak, it)
        }
}

fun Sak.avbrytKlagebehandling(
    kommando: AvbrytKlagebehandlingKommando,
    clock: Clock,
): Either<KanIkkeAvbryteKlagebehandling, Pair<Sak, Klagebehandling>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).avbryt(kommando, clock)
        .map {
            val oppdatertSak = this.oppdaterKlagebehandling(it)
            Pair(oppdatertSak, it)
        }
}
