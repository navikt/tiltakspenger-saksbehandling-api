package no.nav.tiltakspenger.saksbehandling.klage.domene.overta

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.overta.KunneIkkeOvertaBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.overta.OvertaRammebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import java.time.Clock

/**
 * @param overtaRammebehandling Funksjon for å overta rammebehandling, brukes dersom klagebehandlingen har en tilknyttet rammebehandling.
 */
suspend fun Sak.overtaKlagebehandling(
    kommando: OvertaKlagebehandlingKommando,
    clock: Clock,
    overtaRammebehandling: suspend (OvertaRammebehandlingKommando) -> Either<KunneIkkeOvertaBehandling, Pair<Sak, Rammebehandling>>,
    lagre: suspend (Klagebehandling, Statistikkhendelser) -> Unit,
): Either<KanIkkeOvertaKlagebehandling, Triple<Sak, Klagebehandling, Rammebehandling?>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).let { klagebehandling ->
        val rammebehandling = klagebehandling.tilknyttetBehandlingId.let { rammebehandlingId ->
            rammebehandlingId.map { this.hentRammebehandling(it) }.singleOrNullOrThrow { it?.erUnderAktivBehandling == true }
        }
        if (rammebehandling != null) {
            return overtaRammebehandling(
                OvertaRammebehandlingKommando(
                    sakId = kommando.sakId,
                    behandlingId = rammebehandling.id,
                    overtarFra = kommando.overtarFra,
                    saksbehandler = kommando.saksbehandler,
                    correlationId = kommando.correlationId,
                ),
            ).map {
                Triple(it.first, it.second.klagebehandling!!, it.second)
            }.mapLeft {
                KanIkkeOvertaKlagebehandling.KunneIkkeOvertaRammebehandling(it)
            }
        }
        klagebehandling.overta(kommando, null, clock)
            .map { (oppdatertKlagebehandling, statistikkhendelser) ->
                val oppdatertSak = this.oppdaterKlagebehandling(oppdatertKlagebehandling)
                lagre(oppdatertKlagebehandling, statistikkhendelser)
                Triple(oppdatertSak, oppdatertKlagebehandling, null)
            }
    }
}
