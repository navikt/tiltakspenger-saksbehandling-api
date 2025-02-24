package no.nav.tiltakspenger.saksbehandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.domene.behandling.SendRevurderingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.domene.behandling.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.domene.behandling.sendFørstegangsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.domene.behandling.sendRevurderingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService

class SendBehandlingTilBeslutningService(
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
) {
    suspend fun sendFørstegangsbehandlingTilBeslutning(
        kommando: SendSøknadsbehandlingTilBeslutningKommando,
    ): Either<KanIkkeSendeTilBeslutter, Behandling> {
        val sak: Sak =
            sakService.hentForSakId(kommando.sakId, kommando.saksbehandler, kommando.correlationId).getOrElse {
                throw IllegalStateException("Saksbehandler ${kommando.saksbehandler.navIdent} har ikke tilgang til sak ${kommando.sakId}")
            }
        return sak.sendFørstegangsbehandlingTilBeslutning(kommando).map { (_, behandling) -> behandling }.onRight {
            behandlingRepo.lagre(it)
        }
    }

    suspend fun sendRevurderingTilBeslutning(kommando: SendRevurderingTilBeslutningKommando): Either<KanIkkeSendeTilBeslutter, Behandling> {
        val sak: Sak =
            sakService.hentForSakId(kommando.sakId, kommando.saksbehandler, kommando.correlationId).getOrElse {
                throw IllegalStateException("Saksbehandler ${kommando.saksbehandler.navIdent} har ikke tilgang til sak ${kommando.sakId}")
            }

        return sak.sendRevurderingTilBeslutning(kommando).onRight {
            behandlingRepo.lagre(it)
        }
    }
}
