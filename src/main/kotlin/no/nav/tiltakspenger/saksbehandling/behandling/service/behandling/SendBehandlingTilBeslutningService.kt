package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendRevurderingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.sendFørstegangsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.behandling.domene.sendRevurderingTilBeslutning
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
) {
    suspend fun sendFørstegangsbehandlingTilBeslutning(
        kommando: SendSøknadsbehandlingTilBeslutningKommando,
    ): Either<KanIkkeSendeTilBeslutter, Behandling> {
        val sak: Sak =
            sakService.hentForSakId(kommando.sakId, kommando.saksbehandler, kommando.correlationId).getOrElse {
                throw IllegalStateException("Saksbehandler ${kommando.saksbehandler.navIdent} har ikke tilgang til sak ${kommando.sakId}")
            }
        return sak.sendFørstegangsbehandlingTilBeslutning(kommando, clock).map { (_, behandling) -> behandling }.onRight {
            val statistikk = statistikkSakService.genererStatistikkForSendTilBeslutter(it)
            behandlingRepo.lagre(it)
            statistikkSakRepo.lagre(statistikk)
        }
    }

    suspend fun sendRevurderingTilBeslutning(kommando: SendRevurderingTilBeslutningKommando): Either<KanIkkeSendeTilBeslutter, Behandling> {
        val sak: Sak =
            sakService.hentForSakId(kommando.sakId, kommando.saksbehandler, kommando.correlationId).getOrElse {
                throw IllegalStateException("Saksbehandler ${kommando.saksbehandler.navIdent} har ikke tilgang til sak ${kommando.sakId}")
            }

        return sak.sendRevurderingTilBeslutning(kommando, clock).onRight {
            val statistikk = statistikkSakService.genererStatistikkForSendTilBeslutter(it)
            behandlingRepo.lagre(it)
            statistikkSakRepo.lagre(statistikk)
        }
    }
}
