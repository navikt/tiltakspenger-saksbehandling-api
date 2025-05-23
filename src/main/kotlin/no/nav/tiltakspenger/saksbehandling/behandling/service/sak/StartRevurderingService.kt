package no.nav.tiltakspenger.saksbehandling.behandling.service.sak

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.StartRevurderingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.startRevurdering
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.OppdaterSaksopplysningerService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class StartRevurderingService(
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
    private val saksopplysningerService: OppdaterSaksopplysningerService,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {
    val logger = KotlinLogging.logger { }

    suspend fun startRevurdering(
        kommando: StartRevurderingKommando,
    ): Pair<Sak, Behandling> {
        val (sakId, correlationId, saksbehandler) = kommando

        // Denne sjekker tilgang til person og at saksbehandler har rollen SAKSBEHANDLER eller BESLUTTER.
        val sak = sakService.hentForSakIdEllerKast(sakId, saksbehandler, correlationId)

        val (oppdatertSak, behandling) = sak.startRevurdering(
            kommando = kommando,
            clock = clock,
            hentSaksopplysninger = saksopplysningerService::hentSaksopplysningerFraRegistre,
        )

        val statistikk = statistikkSakService.genererStatistikkForRevurdering(behandling)

        sessionFactory.withTransactionContext { tx ->
            behandlingRepo.lagre(behandling, tx)
            statistikkSakRepo.lagre(statistikk, tx)
        }
        return Pair(oppdatertSak, behandling)
    }
}
