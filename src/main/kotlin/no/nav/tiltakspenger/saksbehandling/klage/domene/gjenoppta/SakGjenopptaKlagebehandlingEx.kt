package no.nav.tiltakspenger.saksbehandling.klage.domene.gjenoppta

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenoppta.GjenopptaRammebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.AktivTilknyttetBehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentAktivTilknyttetBehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.gjenoppta.GjenopptaMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import java.time.Clock

suspend fun Sak.gjenopptaKlagebehandling(
    kommando: GjenopptaKlagebehandlingKommando,
    clock: Clock,
    gjenopptaRammebehandling: suspend (GjenopptaRammebehandlingKommando) -> Pair<Sak, Rammebehandling>,
    gjenopptaMeldekortbehandling: (GjenopptaMeldekortbehandlingKommando) -> Pair<Sak, Meldekortbehandling>,
    lagre: suspend (Klagebehandling, Statistikkhendelser) -> Unit,
): Either<KanIkkeGjenopptaKlagebehandling, Triple<Sak, Klagebehandling, AttesterbarBehandling?>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).let { klagebehandling ->
        when (val tilknyttetBehandling = this.hentAktivTilknyttetBehandling(klagebehandling)) {
            is AktivTilknyttetBehandling.Ramme -> {
                // Denne gjenopptar også klagebehandlingen hvis aktuelt.
                gjenopptaRammebehandling(gjenopptaRammebehandling, kommando, tilknyttetBehandling.rammebehandling).right()
            }

            is AktivTilknyttetBehandling.Meldekort -> {
                gjenopptaMeldekortbehandling(gjenopptaMeldekortbehandling, kommando, tilknyttetBehandling.meldekortbehandling).right()
            }

            null -> gjenopptaKlagebehandling(klagebehandling, kommando, clock, lagre)
        }
    }
}

private suspend fun gjenopptaRammebehandling(
    gjenopptaRammebehandling: suspend (GjenopptaRammebehandlingKommando) -> Pair<Sak, Rammebehandling>,
    kommando: GjenopptaKlagebehandlingKommando,
    tilknyttetRammebehandling: Rammebehandling,
): Triple<Sak, Klagebehandling, Rammebehandling> {
    return gjenopptaRammebehandling(
        GjenopptaRammebehandlingKommando(
            sakId = kommando.sakId,
            rammebehandlingId = tilknyttetRammebehandling.id,
            saksbehandler = kommando.saksbehandler,
            correlationId = kommando.correlationId,
        ),
    ).let { Triple(it.first, it.second.klagebehandling!!, it.second) }
}

private fun gjenopptaMeldekortbehandling(
    gjenopptaMeldekortbehandling: (GjenopptaMeldekortbehandlingKommando) -> Pair<Sak, Meldekortbehandling>,
    kommando: GjenopptaKlagebehandlingKommando,
    tilknyttetMeldekortbehandling: Meldekortbehandling,
): Triple<Sak, Klagebehandling, Meldekortbehandling> {
    return gjenopptaMeldekortbehandling(
        GjenopptaMeldekortbehandlingKommando(
            sakId = kommando.sakId,
            meldekortId = tilknyttetMeldekortbehandling.id,
            saksbehandler = kommando.saksbehandler,
            correlationId = kommando.correlationId,
        ),
    ).let { Triple(it.first, it.second.klagebehandling!!, it.second) }
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
