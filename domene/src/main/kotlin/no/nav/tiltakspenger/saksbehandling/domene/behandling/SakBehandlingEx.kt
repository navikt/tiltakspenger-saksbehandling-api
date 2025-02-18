package no.nav.tiltakspenger.saksbehandling.domene.behandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak

fun Sak.sendBehandlingTilBeslutter(
    kommando: SendBehandlingTilBeslutterKommando,
): Either<KanIkkeSendeTilBeslutter, Pair<Sak, Behandling>> {
    if (!kommando.saksbehandler.erSaksbehandler()) {
        return KanIkkeSendeTilBeslutter.MåVæreSaksbehandler.left()
    }
    val behandling: Behandling = this.hentBehandling(kommando.behandlingId)!!
    val oppdatertBehandling = behandling.tilBeslutningV2(kommando)
    return (this.copy(behandlinger = this.behandlinger.oppdaterBehandling(oppdatertBehandling)) to oppdatertBehandling).right()
}
