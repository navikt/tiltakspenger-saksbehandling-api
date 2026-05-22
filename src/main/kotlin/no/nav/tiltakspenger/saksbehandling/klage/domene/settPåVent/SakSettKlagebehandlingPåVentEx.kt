package no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent.SettRammebehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.AktivTilknyttetBehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentAktivTilknyttetBehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppdaterKlagebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent.SettMeldekortbehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import java.time.Clock

suspend fun Sak.settKlagebehandlingPåVent(
    kommando: SettKlagebehandlingPåVentKommando,
    clock: Clock,
    settRammebehandlingPåVent: suspend (SettRammebehandlingPåVentKommando) -> Pair<Sak, Rammebehandling>,
    settMeldekortbehandlingPåVent: (SettMeldekortbehandlingPåVentKommando) -> Pair<Sak, Meldekortbehandling>,
    lagre: suspend (Klagebehandling, Statistikkhendelser) -> Unit,
): Either<KanIkkeSetteKlagebehandlingPåVent, Triple<Sak, Klagebehandling, AttesterbarBehandling?>> {
    return this.hentKlagebehandling(kommando.klagebehandlingId).let { klagebehandling ->
        when (val tilknyttetBehandling = this.hentAktivTilknyttetBehandling(klagebehandling)) {
            is AktivTilknyttetBehandling.Ramme -> return settRammebehandlingPåVent(
                SettRammebehandlingPåVentKommando(
                    sakId = kommando.sakId,
                    rammebehandlingId = tilknyttetBehandling.rammebehandling.id,
                    begrunnelse = kommando.begrunnelse,
                    frist = kommando.frist,
                    saksbehandler = kommando.saksbehandler,
                ),
            ).let { Triple(it.first, it.second.klagebehandling!!, it.second) }.right()

            is AktivTilknyttetBehandling.Meldekort -> return settMeldekortbehandlingPåVent(
                SettMeldekortbehandlingPåVentKommando(
                    sakId = kommando.sakId,
                    meldekortId = tilknyttetBehandling.meldekortbehandling.id,
                    begrunnelse = kommando.begrunnelse,
                    frist = kommando.frist,
                    saksbehandler = kommando.saksbehandler,
                    correlationId = CorrelationId.generate(),
                ),
            ).let { Triple(it.first, it.second.klagebehandling!!, it.second) }.right()

            null -> Unit
        }
        klagebehandling.settPåVent(kommando, clock).map { (oppdatertKlagebehandling, statistikkhendelser) ->
            val oppdatertSak = this.oppdaterKlagebehandling(oppdatertKlagebehandling)
            lagre(oppdatertKlagebehandling, statistikkhendelser)
            Triple(oppdatertSak, oppdatertKlagebehandling, null)
        }
    }
}
