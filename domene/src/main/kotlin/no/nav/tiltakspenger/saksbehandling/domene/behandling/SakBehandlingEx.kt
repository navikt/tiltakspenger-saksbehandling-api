package no.nav.tiltakspenger.saksbehandling.domene.behandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak

fun Sak.sendBehandlingTilBeslutning(
    kommando: SendBehandlingTilBeslutterKommando,
): Either<KanIkkeSendeTilBeslutter, Pair<Sak, Behandling>> {
    if (!kommando.saksbehandler.erSaksbehandler()) {
        return KanIkkeSendeTilBeslutter.MåVæreSaksbehandler.left()
    }
    val behandling: Behandling = this.hentBehandling(kommando.behandlingId)!!
    val oppdatertBehandling = behandling.tilBeslutningV2(kommando)
    return (this.copy(behandlinger = this.behandlinger.oppdaterBehandling(oppdatertBehandling)) to oppdatertBehandling).right()
}

fun Sak.sendRevurderingTilBeslutning(
    kommando: SendRevurderingTilBeslutningKommando,
): Either<KanIkkeSendeTilBeslutter, Behandling> {
    if (!kommando.saksbehandler.erSaksbehandler()) {
        return KanIkkeSendeTilBeslutter.MåVæreSaksbehandler.left()
    }

    val stansDato = kommando.stansDato

    this.førsteLovligeStansdato()?.also {
        if (stansDato.isBefore(it)) {
            throw IllegalArgumentException("Angitt stansdato $stansDato er før første lovlige stansdato $it")
        }
    }

    if (stansDato.isBefore(this.førsteInnvilgetDato)) {
        throw IllegalArgumentException("Kan ikke starte revurdering ($stansDato) før første innvilgetdato (${this.førsteInnvilgetDato})")
    }

    val behandling: Behandling = this.hentBehandling(kommando.behandlingId)!!
    val oppdatertBehandling = behandling.sendRevurderingTilBeslutning(kommando, this.vedtaksperiode!!)

    return oppdatertBehandling.right()
}
