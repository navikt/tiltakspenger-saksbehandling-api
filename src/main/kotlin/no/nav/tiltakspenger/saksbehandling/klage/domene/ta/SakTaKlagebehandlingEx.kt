package no.nav.tiltakspenger.saksbehandling.klage.domene.ta

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.AktivTilknyttetBehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentAktivTilknyttetBehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import java.time.LocalDateTime

suspend fun Sak.taKlagebehandling(
    kommando: TaKlagebehandlingKommando,
    sistEndret: LocalDateTime,
    taRammebehandling: suspend (SakId, RammebehandlingId, Saksbehandler) -> Pair<Sak, Rammebehandling>,
    taMeldekortbehandling: (SakId, MeldekortId, Saksbehandler) -> Pair<Sak, Meldekortbehandling>,
    lagre: suspend (Klagebehandling, Statistikkhendelser) -> Unit,
): Either<KanIkkeTaKlagebehandling, Triple<Sak, Klagebehandling, AttesterbarBehandling?>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).let { klagebehandling ->
        when (val tilknyttetBehandling = this.hentAktivTilknyttetBehandling(klagebehandling)) {
            is AktivTilknyttetBehandling.Ramme -> return taRammebehandling(
                kommando.sakId,
                tilknyttetBehandling.rammebehandling.id,
                kommando.saksbehandler,
            ).let { Triple(it.first, it.second.klagebehandling!!, it.second) }.right()

            is AktivTilknyttetBehandling.Meldekort -> return taMeldekortbehandling(
                kommando.sakId,
                tilknyttetBehandling.meldekortbehandling.id,
                kommando.saksbehandler,
            ).let { Triple(it.first, it.second.klagebehandling!!, it.second) }.right()

            null -> Unit
        }
        klagebehandling
            .ta(
                kommando = kommando,
                tilknyttetBehandlingsstatus = null,
                sistEndret = sistEndret,
            )
            .map { (oppdaterKlagebehandling, statistikkhendelser) ->
                val oppdatertSak = this.oppdaterKlagebehandling(oppdaterKlagebehandling)
                lagre(oppdaterKlagebehandling, statistikkhendelser)
                Triple(oppdatertSak, oppdaterKlagebehandling, null)
            }
    }
}
