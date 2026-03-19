package no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import java.time.Clock

fun Sak.avbrytKlagebehandling(
    kommando: AvbrytKlagebehandlingKommando,
    clock: Clock,
): Either<KanIkkeAvbryteKlagebehandling, Triple<Sak, Klagebehandling, Statistikkhendelser>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId)
        .avbryt(kommando, clock)
        .map { (oppdaterKlagebehandling, statistikkhendelser) ->
            val oppdatertSak = this.oppdaterKlagebehandling(oppdaterKlagebehandling)
            Triple(oppdatertSak, oppdaterKlagebehandling, statistikkhendelser)
        }
}
