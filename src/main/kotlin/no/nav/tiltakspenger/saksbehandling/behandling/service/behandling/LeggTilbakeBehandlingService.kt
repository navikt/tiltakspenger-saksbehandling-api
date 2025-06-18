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
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevSaksbehandlerEllerBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.krevTilgangTilPerson
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService

class LeggTilbakeBehandlingService(
    private val tilgangsstyringService: TilgangsstyringService,
    private val behandlingRepo: BehandlingRepo,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {
    val logger = KotlinLogging.logger { }

    suspend fun leggTilbakeBehandling(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Behandling {
        krevSaksbehandlerEllerBeslutterRolle(saksbehandler)

        val behandling = behandlingRepo.hent(behandlingId)
        tilgangsstyringService.krevTilgangTilPerson(saksbehandler, behandling.fnr, correlationId)

        return behandling.leggTilbakeBehandling(saksbehandler).also {
            when (it.status) {
                Behandlingsstatus.KLAR_TIL_BEHANDLING -> {
                    val statistikk = statistikkSakService.genererStatistikkForOppdatertSaksbehandlerEllerBeslutter(it)
                    sessionFactory.withTransactionContext { tx ->
                        behandlingRepo.leggTilbakeBehandlingSaksbehandler(
                            it.id,
                            saksbehandler,
                            it.status,
                            tx,
                        )
                        statistikkSakRepo.lagre(statistikk, tx)
                    }
                }

                Behandlingsstatus.KLAR_TIL_BESLUTNING -> {
                    val statistikk = statistikkSakService.genererStatistikkForOppdatertSaksbehandlerEllerBeslutter(it)
                    sessionFactory.withTransactionContext { tx ->
                        behandlingRepo.leggTilbakeBehandlingBeslutter(
                            it.id,
                            saksbehandler,
                            it.status,
                            tx,
                        )
                        statistikkSakRepo.lagre(statistikk, tx)
                    }
                }

                Behandlingsstatus.UNDER_BEHANDLING,
                Behandlingsstatus.UNDER_BESLUTNING,
                Behandlingsstatus.VEDTATT,
                Behandlingsstatus.AVBRUTT,
                Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING,
                -> throw IllegalStateException("Behandlingen er i en ugyldig status for å kunne legge tilbake")
            }
        }
    }
}
