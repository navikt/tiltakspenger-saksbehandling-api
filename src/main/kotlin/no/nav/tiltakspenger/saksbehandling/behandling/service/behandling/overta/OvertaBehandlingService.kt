package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.BehandlingService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class OvertaBehandlingService(
    private val behandlingService: BehandlingService,
    private val behandlingRepo: BehandlingRepo,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {

    suspend fun overta(kommando: OvertaBehandlingKommando): Either<KunneIkkeOvertaBehandling, Pair<Sak, Behandling>> {
        val (sakId, behandlingId, overtarFra, saksbehandler, _) = kommando

        val (sak, behandling) = behandlingService.hentSakOgBehandling(
            sakId = sakId,
            behandlingId = behandlingId,
        )

        return behandling.overta(saksbehandler, clock).map {
            val oppdatertSak = sak.oppdaterBehandling(it)
            val statistikk = statistikkSakService.genererStatistikkForOppdatertSaksbehandlerEllerBeslutter(it)

            when (it.status) {
                Behandlingsstatus.UNDER_BEHANDLING -> {
                    sessionFactory.withTransactionContext { tx ->
                        behandlingRepo.overtaSaksbehandler(
                            it.id,
                            saksbehandler,
                            overtarFra,
                            tx,
                        )
                        statistikkSakRepo.lagre(statistikk, tx)
                    }
                }

                Behandlingsstatus.UNDER_BESLUTNING -> {
                    sessionFactory.withTransactionContext { tx ->
                        behandlingRepo.overtaBeslutter(
                            it.id,
                            saksbehandler,
                            overtarFra,
                            tx,
                        )
                        statistikkSakRepo.lagre(statistikk, tx)
                    }
                }

                Behandlingsstatus.KLAR_TIL_BESLUTNING,
                Behandlingsstatus.KLAR_TIL_BEHANDLING,
                Behandlingsstatus.VEDTATT,
                Behandlingsstatus.AVBRUTT,
                Behandlingsstatus.UNDER_AUTOMATISK_BEHANDLING,
                -> throw IllegalStateException("Behandlingen er i en ugyldig status for å kunne overta")
            }

            oppdatertSak to it
        }
    }
}
