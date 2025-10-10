package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.infra.metrikker.MetricRegister
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import no.nav.tiltakspenger.saksbehandling.vedtak.Vedtakstype
import java.time.Clock

class BehandleSøknadPåNyttService(
    private val clock: Clock,
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
    private val hentSaksopplysingerService: HentSaksopplysingerService,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {
    val logger = KotlinLogging.logger { }

    suspend fun startSøknadsbehandlingPåNytt(
        søknadId: SøknadId,
        sakId: SakId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Pair<Sak, Søknadsbehandling> {
        val sak = sakService.hentForSakId(sakId)
        // Merk at i teorien kan en søknad være knyttet til flere avslåtte behandlinger, men i praksis bør det tilnærmet ikke skje.
        val søknad = sak.vedtaksliste.hentAvslåtteBehandlingerForSøknadId(søknadId).single().søknad

        val søknadsbehandling = Søknadsbehandling.opprett(
            sak = sak,
            søknad = søknad,
            saksbehandler = saksbehandler,
            hentSaksopplysninger = hentSaksopplysingerService::hentSaksopplysningerFraRegistre,
            correlationId = correlationId,
            clock = clock,
        )

        val opprettetBehandlingStatistikk = statistikkSakService.genererStatistikkForSøknadsbehandling(
            behandling = søknadsbehandling,
        )

        val opprettetBehandlingPåNyttStatistikk = statistikkSakService.genererStatistikkForSøknadSomBehandlesPåNytt(
            behandling = søknadsbehandling,
        )

        sessionFactory.withTransactionContext { tx ->
            behandlingRepo.lagre(søknadsbehandling, tx)
            statistikkSakRepo.lagre(opprettetBehandlingStatistikk, tx)
            statistikkSakRepo.lagre(opprettetBehandlingPåNyttStatistikk, tx)
            sakService.oppdaterSkalSendesTilMeldekortApi(
                sakId = sakId,
                skalSendesTilMeldekortApi = true,
                sessionContext = tx,
            )
        }
        val oppdatertSak = sak.leggTilSøknadsbehandling(søknadsbehandling)
        MetricRegister.STARTET_BEHANDLING.inc()
        MetricRegister.SØKNAD_BEHANDLET_PÅ_NYTT.inc()
        return (oppdatertSak to søknadsbehandling)
    }
}
