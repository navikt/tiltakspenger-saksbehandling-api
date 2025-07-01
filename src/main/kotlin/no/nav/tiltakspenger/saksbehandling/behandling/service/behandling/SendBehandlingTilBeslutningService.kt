package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.sendRevurderingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.behandling.domene.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class SendBehandlingTilBeslutningService(
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {
    suspend fun sendSøknadsbehandlingTilBeslutning(
        kommando: SendSøknadsbehandlingTilBeslutningKommando,
    ): Either<KanIkkeSendeTilBeslutter, Behandling> {
        val sak: Sak = sakService.sjekkTilgangOgHentForSakId(
            sakId = kommando.sakId,
            saksbehandler = kommando.saksbehandler,
            correlationId = kommando.correlationId,
        )
        // Denne validerer saksbehandler
        return sak.sendSøknadsbehandlingTilBeslutning(kommando, clock).map { (_, behandling) -> behandling }.onRight {
            val statistikk = statistikkSakService.genererStatistikkForSendTilBeslutter(it)
            sessionFactory.withTransactionContext { tx ->
                behandlingRepo.lagre(it, tx)
                statistikkSakRepo.lagre(statistikk, tx)
            }
        }
    }

    suspend fun sendRevurderingTilBeslutning(kommando: RevurderingTilBeslutningKommando): Either<KanIkkeSendeTilBeslutter, Behandling> {
        // Denne sjekker tilgang til person og rollene SAKSBEHANDLER eller BESLUTTER.
        val sak: Sak = sakService.sjekkTilgangOgHentForSakId(
            sakId = kommando.sakId,
            saksbehandler = kommando.saksbehandler,
            correlationId = kommando.correlationId,
        )
        // Denne validerer saksbehandler
        return sak.sendRevurderingTilBeslutning(kommando, clock).onRight {
            val statistikk = statistikkSakService.genererStatistikkForSendTilBeslutter(it)
            sessionFactory.withTransactionContext { tx ->
                behandlingRepo.lagre(it, tx)
                statistikkSakRepo.lagre(statistikk, tx)
            }
        }
    }
}
