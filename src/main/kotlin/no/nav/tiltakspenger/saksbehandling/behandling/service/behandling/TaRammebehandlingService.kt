package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class TaRammebehandlingService(
    private val behandlingService: RammebehandlingService,
    private val rammebehandlingRepo: RammebehandlingRepo,
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
        val (sak, behandling) = behandlingService.hentSakOgBehandling(sakId, behandlingId)

        return behandling.taBehandling(saksbehandler, clock).let {
            val oppdatertSak = sak.oppdaterRammebehandling(it)
            val statistikk = statistikkSakService.genererStatistikkForOppdatertSaksbehandlerEllerBeslutter(it)

            sessionFactory.withTransactionContext { tx ->
                require(
                    when (it.status) {
                        Rammebehandlingsstatus.UNDER_BEHANDLING -> rammebehandlingRepo.taBehandlingSaksbehandler(it, tx)
                        Rammebehandlingsstatus.UNDER_BESLUTNING -> rammebehandlingRepo.taBehandlingBeslutter(it, tx)
                        else -> throw IllegalStateException("Vi havnet i en ugyldig tilstand etter vi tok behandlingen - behandlingId: ${it.id}, status: ${it.status}")
                    },
                ) { "Oppdatering av saksbehandler i db feilet ved ta behandling for $behandlingId" }
                statistikkSakRepo.lagre(statistikk, tx)
            }
            oppdatertSak to it
        }
    }
}
