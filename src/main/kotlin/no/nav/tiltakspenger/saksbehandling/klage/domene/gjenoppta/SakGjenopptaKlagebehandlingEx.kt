package no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenoppta.GjenopptaRammebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.KunneIkkeGjenopptaBehandling
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

suspend fun Sak.gjenopptaKlagebehandling(
    kommando: GjenopptaKlagebehandlingKommando,
    clock: Clock,
    gjenopptaRammebehandling: suspend (GjenopptaRammebehandlingKommando) -> Either<KunneIkkeGjenopptaBehandling, Pair<Sak, Rammebehandling>>,
    lagreKlagebehandling: (Klagebehandling, SessionContext?) -> Unit,
): Either<KanIkkeGjenopptaKlagebehandling, Triple<Sak, Klagebehandling, Rammebehandling?>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).let {
        val rammebehandling = it.rammebehandlingId?.let { this.hentRammebehandling(it) }
        if (rammebehandling != null) {
            return gjenopptaRammebehandling(
                GjenopptaRammebehandlingKommando(
                    sakId = kommando.sakId,
                    rammebehandlingId = rammebehandling.id,
                    saksbehandler = kommando.saksbehandler,
                    correlationId = kommando.correlationId,
                ),
            ).getOrThrow().let {
                Triple(it.first, it.second.klagebehandling!!, it.second)
            }.right()
        }
        it.gjenopptaKlagebehandling(kommando, clock).map {
            val oppdatertSak = this.oppdaterKlagebehandling(it)
            Triple(oppdatertSak, it, null)
        }.onRight { lagreKlagebehandling(it.second, null) }
    }
}
