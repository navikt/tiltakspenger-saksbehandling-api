package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
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
        sak: Sak = sakService.hentForSakId(kommando.sakId),
        transactionContext: TransactionContext? = null,
    ): Pair<Sak, Søknadsbehandling> {
        require(kommando.sakId == sak.id) { "SakId i kommando (${kommando.sakId}) må være lik SakId til hentet sak (${sak.id})" }
        val (oppdatertSak, søknadsbehandling, statistikk) = sak.startSøknadsbehandlingPåNytt(
            kommando = kommando,
            clock = clock,
            genererStatistikkForSøknadsbehandling = statistikkSakService::genererStatistikkForSøknadsbehandling,
            genererStatistikkForSøknadSomBehandlesPåNytt = statistikkSakService::genererStatistikkForSøknadSomBehandlesPåNytt,
            hentSaksopplysninger = hentSaksopplysingerService::hentSaksopplysningerFraRegistre,
        )
        sessionFactory.withTransactionContext(transactionContext) { tx ->
            rammebehandlingRepo.lagre(søknadsbehandling, tx)
            statistikk.forEach { statistikkSakRepo.lagre(it, tx) }
            sakService.oppdaterSkalSendesTilMeldekortApi(
                sakId = sak.id,
                skalSendesTilMeldekortApi = true,
                sessionContext = tx,
            )
            // TODO jah: Å gjøre om withTransactionContext til suspend function er målet, men krever noen dagers arbeid
            @Suppress("RunBlockingInSuspendFunction")
            runBlocking {
                tx.onSuccess {
                    MetricRegister.STARTET_BEHANDLING.inc()
                    MetricRegister.SØKNAD_BEHANDLET_PÅ_NYTT.inc()
                }
            }
        }
        return (oppdatertSak to søknadsbehandling)
    }
}
