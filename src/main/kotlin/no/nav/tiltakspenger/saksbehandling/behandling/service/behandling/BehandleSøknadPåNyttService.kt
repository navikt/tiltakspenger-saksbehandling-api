package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.StartSøknadsbehandlingPåNyttKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.søknadsbehandling.startSøknadsbehandlingPåNytt
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class BehandleSøknadPåNyttService(
    private val clock: Clock,
    private val sakService: SakService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {
    val logger = KotlinLogging.logger { }

    suspend fun startSøknadsbehandlingPåNytt(
        kommando: StartSøknadsbehandlingPåNyttKommando,
    ): Pair<Sak, Søknadsbehandling> {
        val sakId: SakId = kommando.sakId
        val sak: Sak = sakService.hentForSakId(sakId)
        val (oppdatertSak, søknadsbehandling, statistikk) = sak.startSøknadsbehandlingPåNytt(
            kommando = kommando,
            clock = clock,
            genererStatistikkForSøknadsbehandling = statistikkSakService::genererStatistikkForSøknadsbehandling,
            genererStatistikkForSøknadSomBehandlesPåNytt = statistikkSakService::genererStatistikkForSøknadSomBehandlesPåNytt,
            hentSaksopplysninger = hentSaksopplysingerService::hentSaksopplysningerFraRegistre,
        )
        sessionFactory.withTransactionContext { tx ->
            rammebehandlingRepo.lagre(søknadsbehandling, tx)
            statistikk.forEach { statistikkSakRepo.lagre(it, tx) }
            sakService.oppdaterSkalSendesTilMeldekortApi(
                sakId = sakId,
                skalSendesTilMeldekortApi = true,
                sessionContext = tx,
            )
            tx.onSuccess {
                MetricRegister.STARTET_BEHANDLING.inc()
                MetricRegister.SØKNAD_BEHANDLET_PÅ_NYTT.inc()
            }
        }
        return (oppdatertSak to søknadsbehandling)
    }
}
