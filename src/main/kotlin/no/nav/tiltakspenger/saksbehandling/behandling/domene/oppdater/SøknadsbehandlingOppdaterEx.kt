package no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtbetaling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat.Avslag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat.Innvilgelse
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import java.time.Clock

/**
 * Oppdaterer søknadsbehandlingen.
 * Forutsetningene håndheves av [kanOppdatere], og feilene derfra returneres som venstre-verdi.
 * @param utbetaling null dersom avslag eller dersom behandlingen ikke fører til en beregning.
 */
fun Søknadsbehandling.oppdater(
    kommando: OppdaterSøknadsbehandlingKommando,
    clock: Clock,
    utbetaling: BehandlingUtbetaling?,
    omgjørRammevedtak: OmgjørRammevedtak,
): Either<KanIkkeOppdatereBehandling, Søknadsbehandling> {
    kanOppdatere(kommando.saksbehandler).onLeft { return it.left() }

    val resultat = when (kommando) {
        is OppdaterSøknadsbehandlingKommando.Avslag -> {
            Avslag(
                avslagsgrunner = kommando.avslagsgrunner,
                avslagsperiode = this.søknad.tiltaksdeltakelseperiodeDetErSøktOm(),
            )
        }

        is OppdaterSøknadsbehandlingKommando.Innvilgelse -> {
            Innvilgelse(
                barnetillegg = kommando.barnetillegg,
                innvilgelsesperioder = kommando.tilInnvilgelseperioder(this),
                omgjørRammevedtak = omgjørRammevedtak,
            )
        }

        is OppdaterSøknadsbehandlingKommando.IkkeValgtResultat -> null
    }

    return this.copy(
        sistEndret = nå(clock),
        fritekstTilVedtaksbrev = kommando.fritekstTilVedtaksbrev,
        begrunnelseVilkårsvurdering = kommando.begrunnelseVilkårsvurdering,
        resultat = resultat,
        automatiskSaksbehandlet = kommando.automatiskSaksbehandlet,
        utbetaling = utbetaling,
        skalSendeVedtaksbrev = kommando.skalSendeVedtaksbrev,
    ).also {
        require(it.resultat?.erFerdigutfylt(saksopplysninger) != false) {
            "Behandlingsresultatet må være ferdigutfylt etter vi oppdaterer søknadsbehandlingen"
        }
    }.right()
}
