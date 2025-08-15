package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService

class TaBehandlingService(
    private val behandlingService: BehandlingService,
    private val behandlingRepo: BehandlingRepo,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
    private val statistikkSakService: StatistikkSakService,
) {
    val logger = KotlinLogging.logger { }

    suspend fun taBehandling(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Pair<Sak, Behandling> {
        val (sak, behandling) = behandlingService.hentSakOgBehandling(
            sakId = sakId,
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
            correlationId = correlationId,
        )

        return behandling.taBehandling(saksbehandler).let {
            val oppdatertSak = sak.oppdaterBehandling(it)
            val statistikk = statistikkSakService.genererStatistikkForOppdatertSaksbehandlerEllerBeslutter(it)

            when (it.status) {
                Behandlingsstatus.UNDER_BEHANDLING -> {
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

            oppdatertSak to it
        }
    }
}
