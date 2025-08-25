package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import java.time.Clock

class StartSøknadsbehandlingService(
    private val sakService: SakService,
    private val sessionFactory: SessionFactory,
    private val behandlingRepo: BehandlingRepo,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
) {

    val logger = KotlinLogging.logger {}

    suspend fun opprettAutomatiskSoknadsbehandling(
        soknad: Søknad,
        correlationId: CorrelationId,
    ): Søknadsbehandling {
        val sak = sakService.hentForSakId(soknad.sakId)
        val behandling = Søknadsbehandling.opprettAutomatiskBehandling(
            sak = sak,
            søknad = soknad,
            hentSaksopplysninger = hentSaksopplysingerService::hentSaksopplysningerFraRegistre,
            correlationId = correlationId,
            clock = clock,
        )

        val statistikk = statistikkSakService.genererStatistikkForSøknadsbehandling(
            behandling = behandling,
        )

        sessionFactory.withTransactionContext { tx ->
            behandlingRepo.lagre(behandling, tx)
            statistikkSakRepo.lagre(statistikk, tx)
            sakService.oppdaterSkalSendesTilMeldekortApi(
                sakId = soknad.sakId,
                skalSendesTilMeldekortApi = true,
                sessionContext = tx,
            )
        }

        MetricRegister.STARTET_BEHANDLING.inc()
        return behandling
    }
}
