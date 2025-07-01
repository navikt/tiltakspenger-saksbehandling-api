package no.nav.tiltakspenger.saksbehandling.behandling.domene

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

fun Sak.sendSøknadsbehandlingTilBeslutning(
    kommando: SendSøknadsbehandlingTilBeslutningKommando,
    clock: Clock,
): Either<KanIkkeSendeTilBeslutter, Pair<Sak, Søknadsbehandling>> {
    krevSaksbehandlerRolle(kommando.saksbehandler)

    val behandling = this.hentBehandling(kommando.behandlingId)
    require(behandling is Søknadsbehandling) { "Behandlingen må være en søknadsbehandling, men var: ${behandling?.behandlingstype}" }

    if (behandling.saksbehandler != kommando.saksbehandler.navIdent) {
        return KanIkkeSendeTilBeslutter.BehandlingenEiesAvAnnenSaksbehandler(eiesAvSaksbehandler = behandling.saksbehandler)
            .left()
    }
    if (kommando.resultat == SøknadsbehandlingType.INNVILGELSE &&
        this.utbetalinger.hentUtbetalingerFraPeriode(kommando.behandlingsperiode)
            .isNotEmpty()
    ) {
        return KanIkkeSendeTilBeslutter.InnvilgelsesperiodenOverlapperMedUtbetaltPeriode.left()
    }

    return behandling.tilBeslutning(kommando, clock).mapLeft {
        it
    }.map {
        (this.copy(behandlinger = this.behandlinger.oppdaterBehandling(it)) to it)
    }
}
