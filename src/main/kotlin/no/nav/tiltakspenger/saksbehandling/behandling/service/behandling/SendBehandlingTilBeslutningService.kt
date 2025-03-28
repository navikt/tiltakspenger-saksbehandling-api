package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.SendRevurderingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.SendSøknadsbehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.sendFørstegangsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.sendRevurderingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

class SendBehandlingTilBeslutningService(
    private val sakService: no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService,
    private val behandlingRepo: BehandlingRepo,
    private val clock: Clock,
) {
    suspend fun sendFørstegangsbehandlingTilBeslutning(
        kommando: SendSøknadsbehandlingTilBeslutningKommando,
    ): Either<no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.KanIkkeSendeTilBeslutter, Behandling> {
        val sak: Sak =
            sakService.hentForSakId(kommando.sakId, kommando.saksbehandler, kommando.correlationId).getOrElse {
                throw IllegalStateException("Saksbehandler ${kommando.saksbehandler.navIdent} har ikke tilgang til sak ${kommando.sakId}")
            }
        return sak.sendFørstegangsbehandlingTilBeslutning(kommando, clock).map { (_, behandling) -> behandling }.onRight {
            behandlingRepo.lagre(it)
        }
    }

    suspend fun sendRevurderingTilBeslutning(kommando: SendRevurderingTilBeslutningKommando): Either<no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.KanIkkeSendeTilBeslutter, Behandling> {
        val sak: Sak =
            sakService.hentForSakId(kommando.sakId, kommando.saksbehandler, kommando.correlationId).getOrElse {
                throw IllegalStateException("Saksbehandler ${kommando.saksbehandler.navIdent} har ikke tilgang til sak ${kommando.sakId}")
            }

        return sak.sendRevurderingTilBeslutning(kommando, clock).onRight {
            behandlingRepo.lagre(it)
        }
    }
}
