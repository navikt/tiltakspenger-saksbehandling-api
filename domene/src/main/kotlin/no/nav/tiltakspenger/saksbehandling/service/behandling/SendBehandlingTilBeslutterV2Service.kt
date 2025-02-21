package no.nav.tiltakspenger.saksbehandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.domene.behandling.SendBehandlingTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.domene.behandling.SendRevurderingTilBeslutterKommando
import no.nav.tiltakspenger.saksbehandling.domene.behandling.sendBehandlingTilBeslutter
import no.nav.tiltakspenger.saksbehandling.domene.behandling.sendRevurderingTilBeslutter
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService

class SendBehandlingTilBeslutterV2Service(
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
) {
    suspend fun sendTilBeslutter(
        kommando: SendBehandlingTilBeslutterKommando,
    ): Either<KanIkkeSendeTilBeslutter, Behandling> {
        val sak: Sak =
            sakService.hentForSakId(kommando.sakId, kommando.saksbehandler, kommando.correlationId).getOrElse {
                throw IllegalStateException("Saksbehandler ${kommando.saksbehandler.navIdent} har ikke tilgang til sak ${kommando.sakId}")
            }
        return sak.sendBehandlingTilBeslutter(kommando).map { (_, behandling) -> behandling }.onRight {
            behandlingRepo.lagre(it)
        }
    }

    suspend fun sendRevurderingTilBeslutter(kommando: SendRevurderingTilBeslutterKommando): Either<KanIkkeSendeTilBeslutter, Behandling> {
        val sak: Sak =
            sakService.hentForSakId(kommando.sakId, kommando.saksbehandler, kommando.correlationId).getOrElse {
                throw IllegalStateException("Saksbehandler ${kommando.saksbehandler.navIdent} har ikke tilgang til sak ${kommando.sakId}")
            }

        return sak.sendRevurderingTilBeslutter(kommando).onRight {
            behandlingRepo.lagre(it)
        }
    }
}
