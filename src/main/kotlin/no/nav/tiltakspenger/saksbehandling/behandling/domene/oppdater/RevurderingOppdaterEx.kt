package no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat.Innvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat.Stans
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import java.time.Clock
import java.time.LocalDate

/**
 * Oppdaterer en revurdering som innvilger.
 * Forutsetningene håndheves av [kanOppdatere], og feilene derfra returneres som venstre-verdi.
 */
fun Revurdering.oppdaterInnvilgelse(
    kommando: OppdaterRevurderingKommando.Innvilgelse,
    utbetaling: BehandlingUtbetaling?,
    omgjørRammevedtak: OmgjørRammevedtak,
    clock: Clock,
): Either<KanIkkeOppdatereBehandling, Revurdering> {
    kanOppdatere(kommando.saksbehandler).onLeft { return it.left() }

    require(this.resultat is Innvilgelse)

    return this.copy(
        sistEndret = nå(clock),
        begrunnelseVilkårsvurdering = kommando.begrunnelseVilkårsvurdering,
        fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
        resultat = Innvilgelse(
            innvilgelsesperioder = kommando.tilInnvilgelseperioder(this),
            barnetillegg = kommando.barnetillegg,
            omgjørRammevedtak = omgjørRammevedtak,
        ),
        utbetaling = utbetaling,
        skalSendeVedtaksbrev = kommando.skalSendeVedtaksbrev,
    ).also {
        // TODO jah: Etter omgjøring, fjern denne sjekken, fjern nullstill resultat og påse at dette gjøres ved send til beslutter + iverksett.
        require(it.resultat.erFerdigutfylt(saksopplysninger))
    }.right()
}

/**
 * Oppdaterer en revurdering som stanser.
 * Forutsetningene håndheves av [kanOppdatere], og feilene derfra returneres som venstre-verdi.
 */
fun Revurdering.oppdaterStans(
    kommando: OppdaterRevurderingKommando.Stans,
    førsteDagSomGirRett: LocalDate,
    sisteDagSomGirRett: LocalDate,
    utbetaling: BehandlingUtbetaling?,
    omgjørRammevedtak: OmgjørRammevedtak,
    clock: Clock,
): Either<KanIkkeOppdatereBehandling, Revurdering> {
    kanOppdatere(kommando.saksbehandler).onLeft { return it.left() }

    require(this.resultat is Stans)

    return this.copy(
        sistEndret = nå(clock),
        begrunnelseVilkårsvurdering = kommando.begrunnelseVilkårsvurdering,
        fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
        resultat = Stans(
            valgtHjemmel = kommando.valgteHjemler,
            harValgtStansFraFørsteDagSomGirRett = kommando.harValgtStansFraFørsteDagSomGirRett,
            stansperiode = kommando.utledStansperiode(førsteDagSomGirRett, sisteDagSomGirRett),
            omgjørRammevedtak = omgjørRammevedtak,
        ),
        utbetaling = utbetaling,
        skalSendeVedtaksbrev = kommando.skalSendeVedtaksbrev,
    ).right()
}
