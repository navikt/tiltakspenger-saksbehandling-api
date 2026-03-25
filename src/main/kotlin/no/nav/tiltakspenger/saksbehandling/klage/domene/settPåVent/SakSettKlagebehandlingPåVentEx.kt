package no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import java.time.Clock

suspend fun Sak.settKlagebehandlingPåVent(
    kommando: SettKlagebehandlingPåVentKommando,
    clock: Clock,
    settRammebehandlingPåVent: suspend (SettRammebehandlingPåVentKommando) -> Pair<Sak, Rammebehandling>,
    lagre: suspend (Klagebehandling, Statistikkhendelser) -> Unit,
): Either<KanIkkeSetteKlagebehandlingPåVent, Triple<Sak, Klagebehandling, Rammebehandling?>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).let { klagebehandling ->
        val rammebehandling = klagebehandling.rammebehandlingId.let { rammebehandlingId ->
            rammebehandlingId.map { this.hentRammebehandling(it) }.singleOrNullOrThrow { it?.erUnderAktivBehandling == true }
        }
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
        klagebehandling.settPåVent(kommando, clock).map { (oppdatertKlagebehandling, statistikkhendelser) ->
            val oppdatertSak = this.oppdaterKlagebehandling(oppdatertKlagebehandling)
            lagre(oppdatertKlagebehandling, statistikkhendelser)
            Triple(oppdatertSak, oppdatertKlagebehandling, null)
        }
    }
}
