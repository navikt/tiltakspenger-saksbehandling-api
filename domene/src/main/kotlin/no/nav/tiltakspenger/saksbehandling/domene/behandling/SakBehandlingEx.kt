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

fun Sak.sendRevurderingTilBeslutter(
    kommando: SendRevurderingTilBeslutterKommando,
): Either<KanIkkeSendeTilBeslutter, Behandling> {
    if (!kommando.saksbehandler.erSaksbehandler()) {
        return KanIkkeSendeTilBeslutter.MåVæreSaksbehandler.left()
    }

    val stansDato = kommando.stansDato

    this.sisteUtbetalteMeldekortDag()?.also {
        if (it >= stansDato) {
            throw IllegalArgumentException("Kan ikke stanse for meldeperioder som allerede er utbetalt")
        }
    }

    if (stansDato.isBefore(this.førsteInnvilgetDato)) {
        throw IllegalArgumentException("Kan ikke starte revurdering ($stansDato) før første innvilgetdato (${this.førsteInnvilgetDato})")
    }

    val behandling: Behandling = this.hentBehandling(kommando.behandlingId)!!
    val oppdatertBehandling = behandling.tilRevurdering(kommando, this.vedtaksperiode!!)

    return oppdatertBehandling.right()
}
