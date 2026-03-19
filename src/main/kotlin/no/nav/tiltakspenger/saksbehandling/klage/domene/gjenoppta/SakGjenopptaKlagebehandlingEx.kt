package no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenoppta.GjenopptaRammebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.KunneIkkeGjenopptaBehandling
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import java.time.Clock

suspend fun Sak.gjenopptaKlagebehandling(
    kommando: GjenopptaKlagebehandlingKommando,
    clock: Clock,
    gjenopptaRammebehandling: suspend (GjenopptaRammebehandlingKommando) -> Either<KunneIkkeGjenopptaBehandling, Pair<Sak, Rammebehandling>>,
    lagre: suspend (Klagebehandling, Statistikkhendelser) -> Unit,
): Either<KanIkkeGjenopptaKlagebehandling, Triple<Sak, Klagebehandling, Rammebehandling?>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).let { klagebehandling ->
        val tilknyttetRammebehandling = klagebehandling.rammebehandlingId?.let { this.hentRammebehandling(it) }
        if (tilknyttetRammebehandling != null) {
            // Denne gjenopptar også klagebehandlingen hvis aktuelt.
            gjenopptaRammebehandling(gjenopptaRammebehandling, kommando, tilknyttetRammebehandling)
        } else {
            gjenopptaKlagebehandling(klagebehandling, kommando, clock, lagre)
        }
    }
}

private suspend fun gjenopptaRammebehandling(
    gjenopptaRammebehandling: suspend (GjenopptaRammebehandlingKommando) -> Either<KunneIkkeGjenopptaBehandling, Pair<Sak, Rammebehandling>>,
    kommando: GjenopptaKlagebehandlingKommando,
    tilknyttetRammebehandling: Rammebehandling,
): Either<Nothing, Triple<Sak, Klagebehandling, Rammebehandling>> {
    return gjenopptaRammebehandling(
        GjenopptaRammebehandlingKommando(
            sakId = kommando.sakId,
            rammebehandlingId = tilknyttetRammebehandling.id,
            saksbehandler = kommando.saksbehandler,
            correlationId = kommando.correlationId,
        ),
    ).getOrThrow().let {
        Triple(it.first, it.second.klagebehandling!!, it.second)
    }.right()
}

private suspend fun Sak.gjenopptaKlagebehandling(
    klagebehandling: Klagebehandling,
    kommando: GjenopptaKlagebehandlingKommando,
    clock: Clock,
    lagre: suspend (Klagebehandling, Statistikkhendelser) -> Unit,
): Either<KanIkkeGjenopptaKlagebehandling, Triple<Sak, Klagebehandling, Nothing?>> {
    return klagebehandling.gjenopptaKlagebehandling(kommando, clock)
        .map { (oppdatertKlagebehandling, statistikkhendelser) ->
            val oppdatertSak = this.oppdaterKlagebehandling(oppdatertKlagebehandling)
            lagre(oppdatertKlagebehandling, statistikkhendelser)
            Triple(oppdatertSak, oppdatertKlagebehandling, null)
        }
}
