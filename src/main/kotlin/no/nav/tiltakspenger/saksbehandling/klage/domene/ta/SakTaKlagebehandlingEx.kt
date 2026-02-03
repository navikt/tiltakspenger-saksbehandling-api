package no.nav.tiltakspenger.saksbehandling.klage.domene.ta

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

fun Sak.taKlagebehandling(
    kommando: TaKlagebehandlingKommando,
    clock: Clock,
): Either<KanIkkeTaKlagebehandling, Pair<Sak, Klagebehandling>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).let {
        val rammebehandlingsstatus = it.rammebehandlingId?.let { this.hentRammebehandling(it) }?.status
        it.ta(kommando, rammebehandlingsstatus, clock)
            .map {
                val oppdatertSak = this.oppdaterKlagebehandling(it)
                Pair(oppdatertSak, it)
            }
    }
}
