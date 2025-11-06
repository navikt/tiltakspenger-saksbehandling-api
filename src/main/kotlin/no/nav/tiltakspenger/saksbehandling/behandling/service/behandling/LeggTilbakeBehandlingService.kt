package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class LeggTilbakeBehandlingService(
    private val behandlingService: BehandlingService,
    private val behandlingRepo: BehandlingRepo,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    suspend fun leggTilbakeBehandling(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
    ): Pair<Sak, Rammebehandling> {
        val (sak, behandling) = behandlingService.hentSakOgBehandling(
            sakId = sakId,
            behandlingId = behandlingId,
        )

        return behandling.leggTilbakeBehandling(saksbehandler, clock).let {
            val oppdatertSak = sak.oppdaterRammebehandling(it)
            val statistikk = statistikkSakService.genererStatistikkForOppdatertSaksbehandlerEllerBeslutter(it)

            when (it.status) {
                Rammebehandlingsstatus.KLAR_TIL_BEHANDLING -> {
                    sessionFactory.withTransactionContext { tx ->
                        behandlingRepo.leggTilbakeBehandlingSaksbehandler(
                            it.id,
                            saksbehandler,
                            it.status,
                            it.sistEndret,
                            tx,
                        )
                        statistikkSakRepo.lagre(statistikk, tx)
                    }
                }

                Rammebehandlingsstatus.KLAR_TIL_BESLUTNING -> {
                    sessionFactory.withTransactionContext { tx ->
                        behandlingRepo.leggTilbakeBehandlingBeslutter(
                            it.id,
                            saksbehandler,
                            it.status,
                            it.sistEndret,
                            tx,
                        )
                        statistikkSakRepo.lagre(statistikk, tx)
                    }
                }

                Rammebehandlingsstatus.UNDER_BEHANDLING,
                Rammebehandlingsstatus.UNDER_BESLUTNING,
                Rammebehandlingsstatus.VEDTATT,
                Rammebehandlingsstatus.AVBRUTT,
                Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING,
                -> throw IllegalStateException("Behandlingen er i en ugyldig status for å kunne legge tilbake")
            }

            oppdatertSak to it
        }
    }
}
