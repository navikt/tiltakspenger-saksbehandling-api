package no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling

import arrow.core.Nel
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakDTO
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.Clock

suspend fun Sak.startSøknadsbehandlingPåNytt(
    kommando: StartSøknadsbehandlingPåNyttKommando,
    clock: Clock,
    genererStatistikkForSøknadsbehandling: suspend (behandling: Søknadsbehandling) -> StatistikkSakDTO,
    genererStatistikkForSøknadSomBehandlesPåNytt: suspend (behandling: Søknadsbehandling) -> StatistikkSakDTO,
    hentSaksopplysninger: suspend (Fnr, CorrelationId, TiltaksdeltakelserDetErSøktTiltakspengerFor, List<TiltaksdeltakerId>, Boolean) -> Saksopplysninger,
): Triple<Sak, Søknadsbehandling, Nel<StatistikkSakDTO>> {
    val søknad: Søknad = søknader.single { it.id == kommando.søknadId }
    val klagebehandling: Klagebehandling? = kommando.klagebehandlingId?.let { this.hentKlagebehandling(it) }
    return Søknadsbehandling.opprett(
        sak = this,
        søknadsbehandlingId = kommando.søknadsbehandlingId,
        søknad = søknad,
        saksbehandler = kommando.saksbehandler,
        hentSaksopplysninger = hentSaksopplysninger,
        correlationId = kommando.correlationId,
        klagebehandling = klagebehandling,
        clock = clock,
    ).let {
        Triple(
            it.first,
            it.second,
            Nel.of(
                genererStatistikkForSøknadsbehandling(it.second),
                genererStatistikkForSøknadSomBehandlesPåNytt(it.second),
            ),
        )
    }
}
