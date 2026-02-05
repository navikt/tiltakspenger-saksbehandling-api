package no.nav.tiltakspenger.saksbehandling.klage.domene.vurder

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

fun Sak.vurderKlagebehandling(
    kommando: VurderKlagebehandlingKommando,
    clock: Clock,
): Either<KanIkkeVurdereKlagebehandling, Pair<Sak, Klagebehandling>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).let {
        // TODO jah: Vurder å lage et domeneobjekt som wrapper klagebehandling med rammebehandling.
        val rammebehandlingsstatus = it.rammebehandlingId?.let { this.hentRammebehandling(it) }?.status
        it.vurder(kommando, rammebehandlingsstatus, clock)
            .map {
                val oppdatertSak = this.oppdaterKlagebehandling(it)
                Pair(oppdatertSak, it)
            }
    }
}
