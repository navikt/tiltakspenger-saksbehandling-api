package no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

suspend fun Sak.settKlagebehandlingPåVent(
    kommando: SettKlagebehandlingPåVentKommando,
    clock: Clock,
    settRammebehandlingPåVent: suspend (SettRammebehandlingPåVentKommando) -> Pair<Sak, Rammebehandling>,
    lagreKlagebehandling: (Klagebehandling, SessionContext?) -> Unit,
): Either<KanIkkeSetteKlagebehandlingPåVent, Triple<Sak, Klagebehandling, Rammebehandling?>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).let {
        val rammebehandling = it.rammebehandlingId?.let { this.hentRammebehandling(it) }
        if (rammebehandling != null) {
            return settRammebehandlingPåVent(
                SettRammebehandlingPåVentKommando(
                    sakId = kommando.sakId,
                    rammebehandlingId = rammebehandling.id,
                    begrunnelse = kommando.begrunnelse,
                    frist = kommando.frist,
                    saksbehandler = kommando.saksbehandler,
                ),
            ).let {
                Triple(it.first, it.second.klagebehandling!!, it.second)
            }.right()
        }
        it.settPåVent(kommando, clock).map {
            val oppdatertSak = this.oppdaterKlagebehandling(it)
            Triple(oppdatertSak, it, null)
        }.onRight { lagreKlagebehandling(it.second, null) }
    }
}
