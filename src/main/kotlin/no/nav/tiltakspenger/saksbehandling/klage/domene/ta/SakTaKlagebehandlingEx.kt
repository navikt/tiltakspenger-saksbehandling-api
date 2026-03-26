package no.nav.tiltakspenger.saksbehandling.klage.domene.ta

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import java.time.LocalDateTime

suspend fun Sak.taKlagebehandling(
    kommando: TaKlagebehandlingKommando,
    sistEndret: LocalDateTime,
    taRammebehandling: suspend (SakId, BehandlingId, Saksbehandler) -> Pair<Sak, Rammebehandling>,
    lagre: suspend (Klagebehandling, Statistikkhendelser) -> Unit,
): Either<KanIkkeTaKlagebehandling, Triple<Sak, Klagebehandling, Rammebehandling?>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).let { klagebehandling ->
        val rammebehandling = klagebehandling.rammebehandlingId.let { rammebehandlingId ->
            rammebehandlingId.map { this.hentRammebehandling(it) }.singleOrNullOrThrow { it?.erUnderAktivBehandling == true }
        }
        if (rammebehandling != null) {
            return taRammebehandling(
                kommando.sakId,
                rammebehandling.id,
                kommando.saksbehandler,
            ).let {
                Triple(it.first, it.second.klagebehandling!!, it.second)
            }.right()
        }
        klagebehandling
            .ta(kommando, null, sistEndret)
            .map { (oppdaterKlagebehandling, statistikkhendelser) ->
                val oppdatertSak = this.oppdaterKlagebehandling(oppdaterKlagebehandling)
                lagre(oppdaterKlagebehandling, statistikkhendelser)
                Triple(oppdatertSak, oppdaterKlagebehandling, null)
            }
    }
}
