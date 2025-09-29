package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendBehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.KanIkkeSendeTilBeslutter
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.validerKanIverksetteUtbetaling
import java.time.Clock

class SendBehandlingTilBeslutningService(
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun sendTilBeslutning(
        kommando: SendBehandlingTilBeslutningKommando,
    ): Either<KanIkkeSendeTilBeslutter, Pair<Sak, Rammebehandling>> {
        val sak = sakService.hentForSakId(
            sakId = kommando.sakId,
        )

        val behandling = sak.hentBehandling(kommando.behandlingId)

        requireNotNull(behandling) {
            "Fant ikke behandlingen ${kommando.behandlingId} på sak ${kommando.sakId}"
        }

        behandling.utbetaling?.also { utbetaling ->
            utbetaling.validerKanIverksetteUtbetaling().onLeft {
                logger.error { "Kan ikke iverksette behandling med utbetaling - ${kommando.behandlingId} / $it" }

                return when (it) {
                    KanIkkeIverksetteUtbetaling.SimuleringMangler -> KanIkkeSendeTilBeslutter.MåHaSimuleringAvUtbetaling.left()
                    KanIkkeIverksetteUtbetaling.FeilutbetalingStøttesIkke -> KanIkkeSendeTilBeslutter.StøtterIkkeFeilutbetaling.left()
                    KanIkkeIverksetteUtbetaling.JusteringStøttesIkke -> KanIkkeSendeTilBeslutter.StøtterIkkeUtbetalingJustering.left()
                }
            }
        }

        if (behandling.saksbehandler != kommando.saksbehandler.navIdent) {
            return KanIkkeSendeTilBeslutter.BehandlingenEiesAvAnnenSaksbehandler(eiesAvSaksbehandler = behandling.saksbehandler)
                .left()
        }

        return behandling.tilBeslutning(
            kommando = kommando,
            clock = clock,
        ).map {
            val oppdaterSak = sak.oppdaterBehandling(it)

            val statistikk = statistikkSakService.genererStatistikkForSendTilBeslutter(it)

            sessionFactory.withTransactionContext { tx ->
                behandlingRepo.lagre(it, tx)
                statistikkSakRepo.lagre(statistikk, tx)
            }

            oppdaterSak to it
        }
    }
}
