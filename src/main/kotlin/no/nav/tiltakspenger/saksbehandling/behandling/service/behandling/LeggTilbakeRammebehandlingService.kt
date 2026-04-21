package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import java.time.Clock

class LeggTilbakeRammebehandlingService(
    private val behandlingService: RammebehandlingService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    suspend fun leggTilbakeRammebehandling(
        sakId: SakId,
        behandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler,
    ): Pair<Sak, Rammebehandling> {
        val (sak, rammebehandling) = behandlingService.hentSakOgRammebehandling(
            sakId = sakId,
            behandlingId = behandlingId,
        )
        return rammebehandling
            .leggTilbakeRammebehandling(saksbehandler, clock)
            .let { (oppdatertRammebehandling, statistikkhendelser) ->
                val oppdatertSak = sak.oppdaterRammebehandling(oppdatertRammebehandling)
                val statistikkDTO = statistikkService.generer(statistikkhendelser)
                sessionFactory.withTransactionContext { tx ->
                    rammebehandlingRepo.lagre(oppdatertRammebehandling, tx)
                    statistikkService.lagre(statistikkDTO, tx)
                }
                oppdatertSak to oppdatertRammebehandling
            }
    }
}
