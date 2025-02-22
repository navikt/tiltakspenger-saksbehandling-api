package no.nav.tiltakspenger.saksbehandling.domene.behandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak

fun Sak.sendFørstegangsbehandlingTilBeslutning(
    kommando: SendBehandlingTilBeslutningKommando,
): Either<KanIkkeSendeTilBeslutter, Pair<Sak, Behandling>> {
    if (!kommando.saksbehandler.erSaksbehandler()) {
        return KanIkkeSendeTilBeslutter.MåVæreSaksbehandler.left()
    }
    val behandling: Behandling = this.hentBehandling(kommando.behandlingId)!!
    require(behandling.erFørstegangsbehandling) { "Behandlingen må være en førstegangsbehandling, men var: ${behandling.behandlingstype}" }
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

    if (stansDato.isBefore(this.førsteDagSomGirRett)) {
        throw IllegalArgumentException("Kan ikke starte revurdering ($stansDato) før første innvilgetdato (${this.førsteDagSomGirRett})")
    }

    val behandling: Behandling = this.hentBehandling(kommando.behandlingId)!!
    require(behandling.erRevurdering) { "Finnes egen funksjon for å sende til førstegangbehandling til beslutning" }
    val oppdatertBehandling = behandling.sendRevurderingTilBeslutning(kommando, this.vedtaksperiode!!)

    return oppdatertBehandling.right()
}
