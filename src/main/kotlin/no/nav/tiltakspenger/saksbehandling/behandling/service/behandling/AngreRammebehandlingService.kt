package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BEHANDLING
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

        return behandling.angreBehandling(saksbehandler, clock).mapLeft {
            it
        }.map { (oppdatertRammebehandling, statistikkhendelser) ->
            val oppdatertSak = sak.oppdaterRammebehandling(oppdatertRammebehandling)
            val statistikkDTO = statistikkService.generer(statistikkhendelser)
            sessionFactory.withTransactionContext { tx ->
                when (oppdatertRammebehandling.status) {
                    UNDER_BEHANDLING -> rammebehandlingRepo.angreBehandling(oppdatertRammebehandling, tx)
                    else -> throw IllegalStateException("Vi havnet i en ugyldig tilstand etter vi angret behandlingen - behandlingId: ${oppdatertRammebehandling.id}, status: ${oppdatertRammebehandling.status}")
                }
                statistikkService.lagre(statistikkDTO, tx)
            }
            oppdatertSak to oppdatertRammebehandling
        }
    }
}
