package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendBehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KanIkkeSendeRammebehandlingTilBeslutter
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.OppdaterBeregningOgSimuleringService
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import java.time.Clock

class SendRammebehandlingTilBeslutningService(
    private val sakService: SakService,
    private val oppdaterBeregningOgSimuleringService: OppdaterBeregningOgSimuleringService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun sendTilBeslutning(
        kommando: SendBehandlingTilBeslutningKommando,
    ): Either<KanIkkeSendeRammebehandlingTilBeslutter, Pair<Sak, Rammebehandling>> {
        val behandlingId = kommando.behandlingId

        val sak = sakService.hentForSakId(kommando.sakId)
        val behandling = sak.hentRammebehandling(behandlingId)!!

        if (behandling.saksbehandler != kommando.saksbehandler.navIdent) {
            return KanIkkeSendeRammebehandlingTilBeslutter.BehandlingenEiesAvAnnenSaksbehandler(eiesAvSaksbehandler = behandling.saksbehandler)
                .left()
        }

        val (_, behandlingMedUtbetalingskontroll) = oppdaterBeregningOgSimuleringService.oppdaterUtbetalingskontroll(
            sak,
            behandlingId,
            kommando.saksbehandler,
        ).getOrElse {
            return KanIkkeSendeRammebehandlingTilBeslutter.SimuleringFeil(it).left()
        }

        behandlingMedUtbetalingskontroll.validerKanIverksetteUtbetaling().onLeft {
            logger.error { "Utbetaling på behandlingen har et resultat som vi ikke kan iverksette - ${kommando.behandlingId} / $it" }
            rammebehandlingRepo.lagre(behandlingMedUtbetalingskontroll)
            return KanIkkeSendeRammebehandlingTilBeslutter.UtbetalingFeil(it).left()
        }

        return behandlingMedUtbetalingskontroll.tilBeslutning(
            kommando = kommando,
            clock = clock,
        ).map {
            val oppdaterSak = sak.oppdaterRammebehandling(it)

            val statistikk = statistikkSakService.genererStatistikkForSendTilBeslutter(it)

            sessionFactory.withTransactionContext { tx ->
                rammebehandlingRepo.lagre(it, tx)
                statistikkSakRepo.lagre(statistikk, tx)
            }

            oppdaterSak to it
        }
    }
}
