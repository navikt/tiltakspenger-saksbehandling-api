package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.overta.KunneIkkeOvertaBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.overta.OvertaRammebehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.ports.RammebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.RammebehandlingService
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService
import java.time.Clock

class OvertaRammebehandlingService(
    private val rammebehandlingService: RammebehandlingService,
    private val rammebehandlingRepo: RammebehandlingRepo,
    private val clock: Clock,
    private val statistikkService: StatistikkService,
    private val sessionFactory: SessionFactory,
) {
    suspend fun overta(kommando: OvertaRammebehandlingKommando): Either<KunneIkkeOvertaBehandling, Pair<Sak, Rammebehandling>> {
        val (sakId, behandlingId, overtarFra, saksbehandler, _) = kommando

        val (sak, behandling) = rammebehandlingService.hentSakOgRammebehandling(
            sakId = sakId,
            behandlingId = behandlingId,
        )
        return behandling
            .overta(saksbehandler, kommando.correlationId, clock)
            .map { (oppdatertRammebehandling, statistikkhendelser) ->
                val oppdatertSak = sak.oppdaterRammebehandling(oppdatertRammebehandling)
                val statistikkDTO = statistikkService.generer(statistikkhendelser)
                sessionFactory.withTransactionContext { tx ->
                    when (oppdatertRammebehandling.status) {
                        Rammebehandlingsstatus.UNDER_BEHANDLING -> {
                            rammebehandlingRepo.overtaSaksbehandler(
                                rammebehandling = oppdatertRammebehandling,
                                nåværendeSaksbehandler = overtarFra,
                                transactionContext = tx,
                            )
                        }

                        Rammebehandlingsstatus.UNDER_BESLUTNING -> {
                            rammebehandlingRepo.overtaBeslutter(
                                rammebehandling = oppdatertRammebehandling,
                                nåværendeBeslutter = overtarFra,
                                sessionContext = tx,
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
                        statistikkService.lagre(statistikkDTO, tx)
                    }
                }

                oppdatertSak to oppdatertRammebehandling
            }
    }
}
