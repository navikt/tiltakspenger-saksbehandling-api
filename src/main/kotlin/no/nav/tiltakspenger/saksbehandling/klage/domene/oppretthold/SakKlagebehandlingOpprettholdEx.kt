package no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak

suspend fun Sak.opprettholdKlagebehandling(
    kommando: OpprettholdKlagebehandlingKommando,
): Either<KanIkkeOpprettholdeKlagebehandling, Pair<Sak, Klagebehandling>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).oppretthold(
        kommando = kommando,
    ).map {
        val oppdatertSak = this.oppdaterKlagebehandling(it)
        Pair(oppdatertSak, it)
    }
}
