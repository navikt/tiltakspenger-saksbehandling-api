package no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Saksopplysninger
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkhendelseType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.rammebehandling.genererSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import java.time.Clock

suspend fun Sak.startSøknadsbehandlingPåNytt(
    kommando: StartSøknadsbehandlingPåNyttKommando,
    clock: Clock,
    hentSaksopplysninger: suspend (Fnr, CorrelationId, TiltaksdeltakelserDetErSøktTiltakspengerFor, List<TiltaksdeltakerId>, Boolean) -> Saksopplysninger,
): Triple<Sak, Søknadsbehandling, Statistikkhendelser> {
    val søknad: Søknad = søknader.single { it.id == kommando.søknadId }
    val klagebehandling: Klagebehandling? = kommando.klagebehandlingId?.let { this.hentKlagebehandling(it) }

    val (oppdatertSak, opprettetSøknadsbehandling, opprettetstatistikk) = Søknadsbehandling.opprett(
        sak = this,
        søknadsbehandlingId = kommando.søknadsbehandlingId,
        søknad = søknad,
        saksbehandler = kommando.saksbehandler,
        hentSaksopplysninger = hentSaksopplysninger,
        correlationId = kommando.correlationId,
        klagebehandling = klagebehandling,
        clock = clock,
    )
    val statistikkhendelser = opprettetstatistikk.leggTil(
        opprettetSøknadsbehandling.genererSaksstatistikk(StatistikkhendelseType.SOKNAD_BEHANDLET_PA_NYTT),
    )
    return Triple(oppdatertSak, opprettetSøknadsbehandling, statistikkhendelser)
}
