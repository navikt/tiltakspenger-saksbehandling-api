package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Sak

fun Sak.sendFørstegangsbehandlingTilBeslutning(
    kommando: SendSøknadsbehandlingTilBeslutningKommando,
): Either<KanIkkeSendeTilBeslutter, Pair<Sak, Behandling>> {
    if (!kommando.saksbehandler.erSaksbehandler()) {
        return KanIkkeSendeTilBeslutter.MåVæreSaksbehandler.left()
    }
    val behandling: Behandling = this.hentBehandling(kommando.behandlingId)!!
    require(behandling.erFørstegangsbehandling) { "Behandlingen må være en førstegangsbehandling, men var: ${behandling.behandlingstype}" }
    if (overlapperEllerTilstøterNyInnvilgelsesperiodeMedEksisterende(kommando.innvilgelsesperiode)) {
        return KanIkkeSendeTilBeslutter.PeriodenOverlapperEllerTilstøterMedAnnenBehandling.left()
    }
    val oppdatertBehandling = behandling.tilBeslutning(kommando)
    return (this.copy(behandlinger = this.behandlinger.oppdaterBehandling(oppdatertBehandling)) to oppdatertBehandling).right()
}

private fun Sak.overlapperEllerTilstøterNyInnvilgelsesperiodeMedEksisterende(innvilgelsesperiode: Periode): Boolean =
    this.behandlinger.førstegangsBehandlinger.mapNotNull { it.virkningsperiode }.let { eksisterendePerioder ->
        eksisterendePerioder.any { it.overlapperMed(innvilgelsesperiode) || it.tilstøter(innvilgelsesperiode) }
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
    val oppdatertBehandling = behandling.sendRevurderingTilBeslutning(kommando, this.vedtaksliste.sisteDagSomGirRett!!)

    return oppdatertBehandling.right()
}
