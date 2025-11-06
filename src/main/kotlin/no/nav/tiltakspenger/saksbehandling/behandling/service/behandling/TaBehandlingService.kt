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

class TaBehandlingService(
    private val behandlingService: BehandlingService,
    private val behandlingRepo: BehandlingRepo,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
    private val statistikkSakService: StatistikkSakService,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    suspend fun taBehandling(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
    ): Pair<Sak, Rammebehandling> {
        val (sak, behandling) = behandlingService.hentSakOgBehandling(
            sakId = sakId,
            behandlingId = behandlingId,
        )

        return behandling.taBehandling(saksbehandler, clock).let {
            val oppdatertSak = sak.oppdaterRammebehandling(it)
            val statistikk = statistikkSakService.genererStatistikkForOppdatertSaksbehandlerEllerBeslutter(it)

            sessionFactory.withTransactionContext { tx ->
                when (it.status) {
                    Rammebehandlingsstatus.UNDER_BEHANDLING -> {
                        behandlingRepo.taBehandlingSaksbehandler(
                            it.id,
                            saksbehandler,
                            it.status,
                            it.sistEndret,
                            tx,
                        )
                    }

                    Rammebehandlingsstatus.UNDER_BESLUTNING -> {
                        behandlingRepo.taBehandlingBeslutter(
                            it.id,
                            saksbehandler,
                            it.status,
                            it.sistEndret,
                            tx,
                        )
                    }

                    Rammebehandlingsstatus.KLAR_TIL_BESLUTNING,
                    Rammebehandlingsstatus.KLAR_TIL_BEHANDLING,
                    Rammebehandlingsstatus.VEDTATT,
                    Rammebehandlingsstatus.AVBRUTT,
                    Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING,
                    -> throw IllegalStateException("Behandlingen er i en ugyldig status for å kunne ta behandling")
                }.also { harOvertatt ->
                    require(harOvertatt) {
                        "Oppdatering av saksbehandler i db feilet ved ta behandling for $behandlingId"
                    }
                    statistikkSakRepo.lagre(statistikk, tx)
                }
            }

            oppdatertSak to it
        }
    }
}
