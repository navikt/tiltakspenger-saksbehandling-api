package no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenåpne

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import java.time.LocalDateTime

/**
 * Tar opp igjen søknaden bak en avbrutt søknadsbehandling.
 *
 * Den avbrutte behandlingen står urørt - kalleren oppretter en ny søknadsbehandling på den gjenåpnede søknaden.
 * Forutsetningene håndheves av [kanGjenåpneSøknadsbehandling], og feilene derfra returneres som venstre-verdi.
 *
 * @return saken med den gjenåpnede søknaden, og søknaden som må lagres.
 * Søknaden er `null` dersom den ikke var avbrutt, og det da kun er en ny behandling som skal opprettes.
 */
fun Sak.gjenåpneSøknad(
    kommando: GjenåpneSøknadsbehandlingKommando,
    tidspunkt: LocalDateTime,
): Either<KanIkkeGjenåpneSøknadsbehandling, Pair<Sak, Søknad?>> {
    val behandling = hentRammebehandling(kommando.avbruttBehandlingId)
        ?: return KanIkkeGjenåpneSøknadsbehandling.FantIkkeBehandling(kommando.avbruttBehandlingId).left()

    kanGjenåpneSøknadsbehandling(behandling, kommando.saksbehandler).onLeft { return it.left() }
    val søknad = (behandling as Søknadsbehandling).søknad

    if (!søknad.erAvbrutt) {
        return (this to null).right()
    }

    val gjenåpnetSøknad = søknad.gjenåpne(
        gjenåpnetAv = kommando.saksbehandler,
        begrunnelse = kommando.begrunnelse,
        tidspunkt = tidspunkt,
    )
    return (this.copy(søknader = oppdaterSøknad(gjenåpnetSøknad)) to gjenåpnetSøknad).right()
}

/**
 * Avgjør om [saksbehandler] kan gjenåpne søknaden bak [behandling].
 *
 * Betingelsene er at behandlingen er en avbrutt søknadsbehandling, og at søknaden ikke allerede har en søknadsbehandling som lever - da er den verken avsluttet uten vedtak eller mulig å ta opp igjen.
 *
 * Merk at `Rammebehandling.finnGyldigeKommandoer` kun ser behandlingen og derfor ikke kan sjekke den siste betingelsen; den håndheves her, på vei inn i [gjenåpneSøknad].
 */
fun Sak.kanGjenåpneSøknadsbehandling(
    behandling: Rammebehandling,
    saksbehandler: Saksbehandler,
): Either<KanIkkeGjenåpneSøknadsbehandling, Unit> {
    if (behandling !is Søknadsbehandling) {
        return KanIkkeGjenåpneSøknadsbehandling.BehandlingenErIkkeEnSøknadsbehandling.left()
    }
    if (!behandling.erAvbrutt) {
        return KanIkkeGjenåpneSøknadsbehandling.BehandlingenErIkkeAvbrutt(behandling.status).left()
    }
    if (!saksbehandler.erSaksbehandler) {
        return KanIkkeGjenåpneSøknadsbehandling.MåVæreSaksbehandler.left()
    }
    val harAktivBehandlingPåSøknaden = rammebehandlinger
        .filterIsInstance<Søknadsbehandling>()
        .any { it.søknad.id == behandling.søknad.id && !it.erAvbrutt }
    if (harAktivBehandlingPåSøknaden) {
        return KanIkkeGjenåpneSøknadsbehandling.SøknadenHarEnAktivBehandling.left()
    }
    return Unit.right()
}
