package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import java.time.Clock

class TaRammebehandlingService(
    private val behandlingService: RammebehandlingService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val sessionFactory: SessionFactory,
    private val statistikkService: StatistikkService,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    suspend fun taBehandling(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
    ): Pair<Sak, Rammebehandling> {
        val (sak, behandling) = behandlingService.hentSakOgRammebehandling(sakId, behandlingId)

        return behandling.taBehandling(saksbehandler, clock).let { (oppdatertRammebehandling, statistikkhendelser) ->
            val oppdatertSak = sak.oppdaterRammebehandling(oppdatertRammebehandling)
            val statistikkDTO = statistikkService.generer(statistikkhendelser)
            sessionFactory.withTransactionContext { tx ->
                when (oppdatertRammebehandling.status) {
                    UNDER_BEHANDLING -> rammebehandlingRepo.taBehandlingSaksbehandler(oppdatertRammebehandling, tx)
                    UNDER_BESLUTNING -> rammebehandlingRepo.taBehandlingBeslutter(oppdatertRammebehandling, tx)
                    else -> throw IllegalStateException("Vi havnet i en ugyldig tilstand etter vi tok behandlingen - behandlingId: ${oppdatertRammebehandling.id}, status: ${oppdatertRammebehandling.status}")
                }
                statistikkService.lagre(statistikkDTO, tx)
            }
            oppdatertSak to oppdatertRammebehandling
        }
    }
}
