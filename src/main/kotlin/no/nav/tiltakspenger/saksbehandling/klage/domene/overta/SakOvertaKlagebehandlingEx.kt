package no.nav.tiltakspenger.saksbehandling.klage.domene.overta

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.overta.OvertaRammebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.AktivTilknyttetBehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentAktivTilknyttetBehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta.OvertaMeldekortbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import java.time.Clock

/**
 * @param overtaRammebehandling Funksjon for å overta rammebehandling, brukes dersom klagebehandlingen har en aktiv tilknyttet rammebehandling.
 * @param overtaMeldekortbehandling Funksjon for å overta meldekortbehandling, brukes dersom klagebehandlingen har en aktiv tilknyttet meldekortbehandling.
 */
suspend fun Sak.overtaKlagebehandling(
    kommando: OvertaKlagebehandlingKommando,
    clock: Clock,
    overtaRammebehandling: suspend (OvertaRammebehandlingKommando) -> Either<KanIkkeOvertaKlagebehandling, Pair<Sak, Rammebehandling>>,
    overtaMeldekortbehandling: (OvertaMeldekortbehandlingKommando) -> Either<KanIkkeOvertaKlagebehandling, Pair<Sak, Meldekortbehandling>>,
    lagre: suspend (Klagebehandling, Statistikkhendelser) -> Unit,
): Either<KanIkkeOvertaKlagebehandling, Triple<Sak, Klagebehandling, AttesterbarBehandling?>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).let { klagebehandling ->
        when (val tilknyttetBehandling = this.hentAktivTilknyttetBehandling(klagebehandling)) {
            is AktivTilknyttetBehandling.Ramme -> return overtaRammebehandling(
                OvertaRammebehandlingKommando(
                    sakId = kommando.sakId,
                    behandlingId = tilknyttetBehandling.rammebehandling.id,
                    overtarFra = kommando.overtarFra,
                    saksbehandler = kommando.saksbehandler,
                    correlationId = kommando.correlationId,
                ),
            ).map {
                Triple(it.first, it.second.klagebehandling!!, it.second)
            }

            is AktivTilknyttetBehandling.Meldekort -> return overtaMeldekortbehandling(
                OvertaMeldekortbehandlingKommando(
                    sakId = kommando.sakId,
                    meldekortId = tilknyttetBehandling.meldekortbehandling.id,
                    overtarFra = kommando.overtarFra,
                    saksbehandler = kommando.saksbehandler,
                    correlationId = kommando.correlationId,
                ),
            ).map {
                Triple(it.first, it.second.klagebehandling!!, it.second)
            }

            null -> Unit
        }
        klagebehandling.overta(
            kommando = kommando,
            tilknyttetBehandlingsstatus = null,
            clock = clock,
        )
            .map { (oppdatertKlagebehandling, statistikkhendelser) ->
                val oppdatertSak = this.oppdaterKlagebehandling(oppdatertKlagebehandling)
                lagre(oppdatertKlagebehandling, statistikkhendelser)
                Triple(oppdatertSak, oppdatertKlagebehandling, null)
            }
    }
}
