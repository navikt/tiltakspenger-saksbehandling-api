package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.KanIkkeBehandleSøknadPåNytt
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class BehandleSøknadPåNyttService(
    private val clock: Clock,
    private val behandlingRepo: BehandlingRepo,
    private val sakService: SakService,
    private val startSøknadsbehandlingService: StartSøknadsbehandlingService,
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
    ): Either<KanIkkeBehandleSøknadPåNytt, Søknadsbehandling> {
        val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId)
        val behandling = behandlingRepo.hentForSøknadId(søknadId)

        if (behandling == null) {
            return KanIkkeBehandleSøknadPåNytt.FantIngenBehandlingForSøknad(søknadId).left()
        }

        when (behandling) {
            is Søknadsbehandling -> {
                val søknad = behandling.søknad
                if (behandling.utfall !is SøknadsbehandlingResultat.Avslag && !behandling.erVedtatt) {
                    return KanIkkeBehandleSøknadPåNytt.BehandlingMåVæreVedtattAvslag(behandling.id).left()
                }

                val innvilgedePerioder = sak.vedtaksliste.innvilgetTidslinje.overlapperMed(søknad.vurderingsperiode())
                if (innvilgedePerioder.perioderMedVerdi.isNotEmpty()) {
                    return KanIkkeBehandleSøknadPåNytt.PeriodeOverlapperInnvilgetVedtak(søknadId, innvilgedePerioder).left()
                }

                val opprettetSøknadsbehandling = startSøknadsbehandlingService.startSøknadsbehandling(
                    søknadId = søknadId,
                    sakId = sakId,
                    saksbehandler = saksbehandler,
                    correlationId = correlationId,
                )

                val søknadsbehandling = opprettetSøknadsbehandling.getOrElse {
                    return KanIkkeBehandleSøknadPåNytt.OppretteBehandling(it).left()
                }

                val statistikk = statistikkSakService.genererStatistikkForGjenåpnetSøknadsbehandling(
                    behandling = søknadsbehandling,
                    søknadId = søknadId,
                )

                sessionFactory.withTransactionContext { tx ->
                    statistikkSakRepo.lagre(statistikk, tx)
                }

                return søknadsbehandling.right()
            }

            is Revurdering -> {
                return KanIkkeBehandleSøknadPåNytt.RevurderingKanIkkeBehandlesPåNytt(behandling.id).left()
            }
        }
    }
}
