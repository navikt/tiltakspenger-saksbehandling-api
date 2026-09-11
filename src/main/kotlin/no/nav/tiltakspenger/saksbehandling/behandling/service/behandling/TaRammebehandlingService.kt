package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import arrow.core.toNonEmptySetOrThrow
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
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.TaRammebehandlingerKommando.RammebehandlingMedSakId
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

    suspend fun taRammebehandlinger(
        kommando: TaRammebehandlingerKommando,
    ): Either<KunneIkkeTaBehandling, List<Pair<Sak, Rammebehandling>>> {
        val sakBehandlingOgStatistikk: List<Triple<Sak, Rammebehandling, StatistikkDTO>> = kommando.behandlinger.map {
            val (sak, behandling) = behandlingService.hentSakOgRammebehandling(it.sakId, it.behandlingId)

            behandling.taBehandling(kommando.saksbehandler, clock).getOrElse {
                return@taRammebehandlinger it.left()
            }.let { (oppdatertRammebehandling, statistikkhendelser) ->
                val oppdatertSak = sak.oppdaterRammebehandling(oppdatertRammebehandling)
                val statistikkDTO = statistikkService.generer(statistikkhendelser)

                Triple(oppdatertSak, oppdatertRammebehandling, statistikkDTO)
            }
        }

        sessionFactory.withTransactionContext { tx ->
            sakBehandlingOgStatistikk.forEach { (_, rammebehandling, statistikk) ->
                when (rammebehandling.status) {
                    UNDER_BEHANDLING -> rammebehandlingRepo.taBehandlingSaksbehandler(rammebehandling, tx)
                    UNDER_BESLUTNING -> rammebehandlingRepo.taBehandlingBeslutter(rammebehandling, tx)
                    else -> throw IllegalStateException("Vi havnet i en ugyldig tilstand etter vi tok behandlingen - behandlingId: ${rammebehandling.id}, status: ${rammebehandling.status}")
                }
                statistikkService.lagre(statistikk, tx)
            }
        }

        return sakBehandlingOgStatistikk.map { it.first to it.second }.right()
    }

    suspend fun taRammebehandling(
        sakId: SakId,
        behandlingId: RammebehandlingId,
        saksbehandler: Saksbehandler,
    ): Either<KunneIkkeTaBehandling, Pair<Sak, Rammebehandling>> {
        return taRammebehandlinger(
            TaRammebehandlingerKommando(
                saksbehandler = saksbehandler,
                behandlinger = nonEmptyListOf(
                    RammebehandlingMedSakId(
                        behandlingId = behandlingId,
                        sakId = sakId,
                    ),
                ),
            ),
        ).map { it.single() }
    }
}

data class TaRammebehandlingerKommando(
    val saksbehandler: Saksbehandler,
    val behandlinger: NonEmptyList<RammebehandlingMedSakId>,
) {
    fun hentSakIder(): NonEmptySet<SakId> {
        return behandlinger.map { it.sakId }.toNonEmptySetOrThrow()
    }

    data class RammebehandlingMedSakId(
        val behandlingId: RammebehandlingId,
        val sakId: SakId,
    )
}
