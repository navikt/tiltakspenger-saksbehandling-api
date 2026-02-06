package no.nav.tiltakspenger.saksbehandling.klage.domene.ta

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock
import java.time.LocalDateTime

suspend fun Sak.taKlagebehandling(
    kommando: TaKlagebehandlingKommando,
    sistEndret: LocalDateTime,
    taRammebehandling: suspend (SakId, BehandlingId, Saksbehandler) -> Pair<Sak, Rammebehandling>,
    lagreKlagebehandling: (Klagebehandling, SessionContext?) -> Unit,
): Either<KanIkkeTaKlagebehandling, Triple<Sak, Klagebehandling, Rammebehandling?>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).let {
        val rammebehandling = it.rammebehandlingId?.let { this.hentRammebehandling(it) }
        if (rammebehandling != null) {
            return taRammebehandling(
                kommando.sakId,
                rammebehandling.id,
                kommando.saksbehandler,
            ).let {
                Triple(it.first, it.second.klagebehandling!!, it.second)
            }.right()
        }
        it.ta(kommando, null, sistEndret).map {
            val oppdatertSak = this.oppdaterKlagebehandling(it)
            Triple(oppdatertSak, it, null)
        }.onRight { lagreKlagebehandling(it.second, null) }
    }
}
