package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ta.KunneIkkeTaBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ta.taBehandling
import no.nav.tiltakspenger.saksbehandling.felles.getOrThrow
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkDTO
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
        behandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler,
    ): Either<KunneIkkeTaBehandling, Pair<Sak, Rammebehandling>> {
        val (sak, behandling) = behandlingService.hentSakOgRammebehandling(sakId, behandlingId)

        return behandling.taBehandling(saksbehandler, clock).mapLeft {
            it
        }.map { (oppdatertRammebehandling, statistikkhendelser) ->
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

    suspend fun taFlereBehandlinger(
        kommando: TaRammebehandlingerKommando,
    ): Either<KunneIkkeTaBehandling, Unit> { // Husk å oppdater unit for det frontend trenger

        val behandlingerMedStatistikk: List<Pair<Rammebehandling, StatistikkDTO>> = kommando.behandlinger.map {
            val (sak, behandling) = behandlingService.hentSakOgRammebehandling(it.sakId, it.behandlingId)

            val pair = behandling.taBehandling(kommando.saksbehandler, clock).onLeft {
                return@taFlereBehandlinger it.left()
            }.map { (oppdatertRammebehandling, statistikkhendelser) ->
                // Verifiser at saken er ok
                sak.oppdaterRammebehandling(oppdatertRammebehandling)
                val statistikkDTO = statistikkService.generer(statistikkhendelser)

                oppdatertRammebehandling to statistikkDTO
            }

            pair.getOrThrow()
        }

        sessionFactory.withTransactionContext { tx ->
            behandlingerMedStatistikk.forEach { (rammebehandling, statistikk) ->
                when (rammebehandling.status) {
                    UNDER_BEHANDLING -> rammebehandlingRepo.taBehandlingSaksbehandler(rammebehandling, tx)
                    UNDER_BESLUTNING -> rammebehandlingRepo.taBehandlingBeslutter(rammebehandling, tx)
                    else -> throw IllegalStateException("Vi havnet i en ugyldig tilstand etter vi tok behandlingen - behandlingId: ${rammebehandling.id}, status: ${rammebehandling.status}")
                }
                statistikkService.lagre(statistikk, tx)
            }
        }

        return Unit.right()
    }
}

data class TaRammebehandlingerKommando(
    val saksbehandler: Saksbehandler,
    val behandlinger: List<RammebehandlingMedSakId>,

)

data class RammebehandlingMedSakId(
    val behandlingId: RammebehandlingId,
    val sakId: SakId,
)
