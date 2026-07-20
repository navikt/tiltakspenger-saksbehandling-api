package no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.leggTilbake.KanIkkeLeggeTilbakeRammebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.AktivTilknyttetBehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentAktivTilknyttetBehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.leggTilbake.KanIkkeLeggeTilbakeMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import java.time.Clock

suspend fun Sak.leggTilbakeKlagebehandling(
    kommando: LeggTilbakeKlagebehandlingKommando,
    clock: Clock,
    leggTilbakeRammebehandling: suspend (SakId, RammebehandlingId, Saksbehandler) -> Either<KanIkkeLeggeTilbakeRammebehandling, Pair<Sak, Rammebehandling>>,
    leggTilbakeMeldekortbehandling: (SakId, MeldekortId, Saksbehandler) -> Either<KanIkkeLeggeTilbakeMeldekortbehandling, Pair<Sak, Meldekortbehandling>>,
    lagre: suspend (Klagebehandling, Statistikkhendelser) -> Unit,
): Either<KanIkkeLeggeTilbakeKlagebehandling, Triple<Sak, Klagebehandling, AttesterbarBehandling?>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).let { klagebehandling ->
        when (val tilknyttetBehandling = this.hentAktivTilknyttetBehandling(klagebehandling)) {
            is AktivTilknyttetBehandling.Ramme -> return leggTilbakeRammebehandling(
                kommando.sakId,
                tilknyttetBehandling.rammebehandling.id,
                kommando.saksbehandler,
            ).mapLeft { KanIkkeLeggeTilbakeKlagebehandling.FeilVedRammebehandling(it) }
                .map { Triple(it.first, it.second.klagebehandling!!, it.second) }

            is AktivTilknyttetBehandling.Meldekort -> return leggTilbakeMeldekortbehandling(
                kommando.sakId,
                tilknyttetBehandling.meldekortbehandling.id,
                kommando.saksbehandler,
            ).mapLeft { KanIkkeLeggeTilbakeKlagebehandling.FeilVedMeldekortbehandling(it) }
                .map { Triple(it.first, it.second.klagebehandling!!, it.second) }

            null -> Unit
        }
        klagebehandling.leggTilbake(
            kommando = kommando,
            tilknyttetBehandlingsstatus = null,
            clock = clock,
        ).map { (oppdaterKlagebehandling, statistikkhendelser) ->
            val oppdatertSak = this.oppdaterKlagebehandling(oppdaterKlagebehandling)
            lagre(oppdaterKlagebehandling, statistikkhendelser)
            Triple(oppdatertSak, oppdaterKlagebehandling, null)
        }
    }
}
