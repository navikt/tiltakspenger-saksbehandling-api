package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
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

    suspend fun overta(kommando: OvertaBehandlingKommando): Either<KunneIkkeOvertaBehandling, Pair<Sak, Rammebehandling>> {
        val (sakId, behandlingId, overtarFra, saksbehandler, _) = kommando

        val (sak, behandling) = behandlingService.hentSakOgBehandling(
            sakId = sakId,
            behandlingId = behandlingId,
        )

        return behandling.overta(saksbehandler, clock).map {
            val oppdatertSak = sak.oppdaterRammebehandling(it)
            val statistikk = statistikkSakService.genererStatistikkForOppdatertSaksbehandlerEllerBeslutter(it)

            sessionFactory.withTransactionContext { tx ->
                when (it.status) {
                    Rammebehandlingsstatus.UNDER_BEHANDLING -> {
                        behandlingRepo.overtaSaksbehandler(
                            it.id,
                            saksbehandler,
                            overtarFra,
                            it.sistEndret,
                            tx,
                        )
                    }

                    Rammebehandlingsstatus.UNDER_BESLUTNING -> {
                        behandlingRepo.overtaBeslutter(
                            it.id,
                            saksbehandler,
                            overtarFra,
                            it.sistEndret,
                            tx,
                        )
                    }

                    Rammebehandlingsstatus.KLAR_TIL_BESLUTNING,
                    Rammebehandlingsstatus.KLAR_TIL_BEHANDLING,
                    Rammebehandlingsstatus.VEDTATT,
                    Rammebehandlingsstatus.AVBRUTT,
                    Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING,
                    -> throw IllegalStateException("Behandlingen er i en ugyldig status for å kunne overta")
                }.also { harOvertatt ->
                    require(harOvertatt) {
                        "Oppdatering av saksbehandler i db feilet ved ta behandling for $behandlingId"
                    }
                    statistikkSakRepo.lagre(statistikk, tx)
                }
            }

            oppdatertSak to it
        }
    }
}
