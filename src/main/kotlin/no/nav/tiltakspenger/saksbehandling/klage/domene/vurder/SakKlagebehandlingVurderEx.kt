package no.nav.tiltakspenger.saksbehandling.klage.domene.vurder

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.klage.domene.AktivTilknyttetBehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentAktivTilknyttetBehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.tilTilknyttetBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

fun Sak.vurderKlagebehandling(
    kommando: VurderKlagebehandlingKommando,
    clock: Clock,
): Either<KanIkkeVurdereKlagebehandling, Pair<Sak, Klagebehandling>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).let {
        val tilknyttetBehandlingsstatus = when (val aktivTilknyttetBehandling = this.hentAktivTilknyttetBehandling(it)) {
            is AktivTilknyttetBehandling.Ramme -> aktivTilknyttetBehandling.rammebehandling.status.tilTilknyttetBehandlingsstatus()
            is AktivTilknyttetBehandling.Meldekort -> aktivTilknyttetBehandling.meldekortbehandling.status.tilTilknyttetBehandlingsstatus()
            null -> null
        }
        it.vurder(kommando, tilknyttetBehandlingsstatus, clock)
            .map {
                val oppdatertSak = this.oppdaterKlagebehandling(it)
                Pair(oppdatertSak, it)
            }
    }
}
