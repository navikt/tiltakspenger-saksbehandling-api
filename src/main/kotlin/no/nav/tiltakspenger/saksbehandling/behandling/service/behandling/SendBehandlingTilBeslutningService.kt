package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterBehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterSøknadsbehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.sendRevurderingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.behandling.domene.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class SendBehandlingTilBeslutningService(
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val navkontorService: NavkontorService,
    private val sessionFactory: SessionFactory,
) {
    suspend fun sendTilBeslutning(
        kommando: OppdaterBehandlingKommando,
    ): Either<KanIkkeSendeTilBeslutter, Behandling> {
        krevSaksbehandlerRolle(kommando.saksbehandler)

        val sak = sakService.hentForSakId(
            sakId = kommando.sakId,
            saksbehandler = kommando.saksbehandler,
            correlationId = kommando.correlationId,
        )

        val behandling = sak.hentBehandling(kommando.behandlingId)

        requireNotNull(behandling) {
            "Fant ikke behandlingen ${kommando.behandlingId} på sak ${kommando.sakId}"
        }

        if (behandling.saksbehandler != kommando.saksbehandler.navIdent) {
            return KanIkkeSendeTilBeslutter.BehandlingenEiesAvAnnenSaksbehandler(eiesAvSaksbehandler = behandling.saksbehandler)
                .left()
        }

        return when (kommando) {
            is OppdaterRevurderingKommando -> sak.sendRevurderingTilBeslutning(
                kommando = kommando,
                hentNavkontor = navkontorService::hentOppfolgingsenhet,
                clock = clock,
            )

            is OppdaterSøknadsbehandlingKommando -> sak.sendSøknadsbehandlingTilBeslutning(
                kommando = kommando,
                clock = clock,
            ).map { it.second }
        }.onRight { sak.validerOgLagre(it) }
    }

    private suspend fun Sak.validerOgLagre(behandling: Behandling) {
        this.copy(behandlinger = this.behandlinger.oppdaterBehandling(behandling))

        val statistikk = statistikkSakService.genererStatistikkForSendTilBeslutter(behandling)

        sessionFactory.withTransactionContext { tx ->
            behandlingRepo.lagre(behandling, tx)
            statistikkSakRepo.lagre(statistikk, tx)
        }
    }
}
