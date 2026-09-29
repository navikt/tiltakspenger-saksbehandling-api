package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.angre.KunneIkkeAngreBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.angre.angreBehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import java.time.Clock

class AngreRammebehandlingService(
    private val behandlingService: RammebehandlingService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val sessionFactory: SessionFactory,
    private val statistikkService: StatistikkService,
    private val clock: Clock,
) {
    val logger = KotlinLogging.logger { }

    suspend fun angreBehandling(
        sakId: SakId,
        behandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler,
    ): Either<KunneIkkeAngreBehandling, Pair<Sak, Rammebehandling>> {
        val (sak, behandling) = behandlingService.hentSakOgRammebehandling(sakId, behandlingId)

        return behandling.angreBehandling(saksbehandler, clock).map { (oppdatertRammebehandling, statistikkhendelser) ->
            val oppdatertSak = sak.oppdaterRammebehandling(oppdatertRammebehandling)
            val statistikkDTO = statistikkService.generer(statistikkhendelser)

            val vellykket = sessionFactory.withTransactionContext { tx ->
                val oppdatert = rammebehandlingRepo.angreBehandling(oppdatertRammebehandling, transactionContext = tx)
                if (oppdatert) statistikkService.lagre(statistikkDTO, tx)
                oppdatert
            }

            if (!vellykket) return KunneIkkeAngreBehandling.BehandlingenErIkkeLengerSendtTilBeslutning.left()

            oppdatertSak to oppdatertRammebehandling
        }
    }
}
