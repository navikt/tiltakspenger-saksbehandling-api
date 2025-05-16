package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

fun Sak.sendSøknadsbehandlingTilBeslutning(
    kommando: SendSøknadsbehandlingTilBeslutningKommando,
    clock: Clock,
): Either<KanIkkeSendeTilBeslutter, Pair<Sak, Søknadsbehandling>> {
    if (!kommando.saksbehandler.erSaksbehandler()) {
        return KanIkkeSendeTilBeslutter.MåVæreSaksbehandler.left()
    }

    val behandling = this.hentBehandling(kommando.behandlingId)
    require(behandling is Søknadsbehandling) { "Behandlingen må være en søknadsbehandling, men var: ${behandling?.behandlingstype}" }

    if (overlapperEllerTilstøterNyInnvilgelsesperiodeMedEksisterende(behandling.id, kommando.behandlingsperiode)) {
        return KanIkkeSendeTilBeslutter.PeriodenOverlapperEllerTilstøterMedAnnenBehandling.left()
    }
    if (behandling.saksbehandler != kommando.saksbehandler.navIdent) {
        return KanIkkeSendeTilBeslutter.BehandlingenEiesAvAnnenSaksbehandler(eiesAvSaksbehandler = behandling.saksbehandler)
            .left()
    }
    val oppdatertBehandling = behandling.tilBeslutning(kommando, clock)
    return (this.copy(behandlinger = this.behandlinger.oppdaterBehandling(oppdatertBehandling)) to oppdatertBehandling).right()
}

private fun Sak.overlapperEllerTilstøterNyInnvilgelsesperiodeMedEksisterende(
    behandlingId: BehandlingId,
    innvilgelsesperiode: Periode,
): Boolean =
    this.behandlinger.søknadsbehandlinger.filter { it.id != behandlingId }.mapNotNull { it.virkningsperiode }
        .let { eksisterendePerioder ->
            eksisterendePerioder.any { it.overlapperMed(innvilgelsesperiode) || it.tilstøter(innvilgelsesperiode) }
        }
