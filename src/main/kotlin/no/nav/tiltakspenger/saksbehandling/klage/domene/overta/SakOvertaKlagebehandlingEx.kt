package no.nav.tiltakspenger.saksbehandling.klage.domene.overta

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.KunneIkkeOvertaBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta.OvertaRammebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

/**
 * @param overtaRammebehandling Funksjon for å overta rammebehandling, brukes dersom klagebehandlingen har en tilknyttet rammebehandling.
 */
suspend fun Sak.overtaKlagebehandling(
    kommando: OvertaKlagebehandlingKommando,
    clock: Clock,
    overtaRammebehandling: suspend (OvertaRammebehandlingKommando) -> Either<KunneIkkeOvertaBehandling, Pair<Sak, Rammebehandling>>,
    lagreKlagebehandling: (Klagebehandling, SessionContext?) -> Unit,
): Either<KanIkkeOvertaKlagebehandling, Triple<Sak, Klagebehandling, Rammebehandling?>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).let {
        val rammebehandling = it.rammebehandlingId?.let { this.hentRammebehandling(it) }
        if (rammebehandling != null) {
            return overtaRammebehandling(
                OvertaRammebehandlingKommando(
                    sakId = kommando.sakId,
                    behandlingId = rammebehandling.id,
                    overtarFra = kommando.overtarFra,
                    saksbehandler = kommando.saksbehandler,
                    correlationId = kommando.correlationId,
                ),
            ).getOrNull()!!.let {
                Triple(it.first, it.second.klagebehandling!!, it.second)
            }.right()
        }
        it.overta(kommando, null, clock).map {
            val oppdatertSak = this.oppdaterKlagebehandling(it)
            Triple(oppdatertSak, it, null)
        }.onRight { lagreKlagebehandling(it.second, null) }
    }
}
