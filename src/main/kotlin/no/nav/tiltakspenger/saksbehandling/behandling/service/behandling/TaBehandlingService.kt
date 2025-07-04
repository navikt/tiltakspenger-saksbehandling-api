package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.krevTilgangTilPerson
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService

class TaBehandlingService(
    private val tilgangsstyringService: TilgangsstyringService,
    private val behandlingRepo: BehandlingRepo,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {
    val logger = KotlinLogging.logger { }

    suspend fun taBehandling(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Behandling {
        val behandling = behandlingRepo.hent(behandlingId)
        tilgangsstyringService.krevTilgangTilPerson(saksbehandler, behandling.fnr, correlationId)

        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)

        return behandling.taBehandling(saksbehandler).also {
            when (it.status) {
                Behandlingsstatus.UNDER_BEHANDLING -> {
                    val statistikk = statistikkSakService.genererStatistikkForOppdatertSaksbehandlerEllerBeslutter(it)
                    sessionFactory.withTransactionContext { tx ->
                        behandlingRepo.taBehandlingSaksbehandler(
                            it.id,
                            saksbehandler,
                            it.status,
                            tx,
                        )
                        statistikkSakRepo.lagre(statistikk, tx)
                    }
                }

                Behandlingsstatus.UNDER_BESLUTNING -> {
                    val statistikk = statistikkSakService.genererStatistikkForOppdatertSaksbehandlerEllerBeslutter(it)
                    sessionFactory.withTransactionContext { tx ->
                        behandlingRepo.taBehandlingBeslutter(
                            it.id,
                            saksbehandler,
                            it.status,
                            tx,
                        )
                        statistikkSakRepo.lagre(statistikk, tx)
                    }
                }

                Behandlingsstatus.KLAR_TIL_BESLUTNING,
                Behandlingsstatus.KLAR_TIL_BEHANDLING,
                Behandlingsstatus.VEDTATT,
                Behandlingsstatus.AVBRUTT,
                Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING,
                -> throw IllegalStateException("Behandlingen er i en ugyldig status for å kunne overta")
            }
        }
    }
}
