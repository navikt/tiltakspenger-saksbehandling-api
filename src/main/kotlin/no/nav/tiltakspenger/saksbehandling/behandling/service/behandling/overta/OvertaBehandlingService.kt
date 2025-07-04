package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta

import arrow.core.Either
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import no.nav.tiltakspenger.saksbehandling.felles.krevTilgangTilPerson
import no.nav.tiltakspenger.saksbehandling.statistikk.behandling.StatistikkSakService
import java.time.Clock

class OvertaBehandlingService(
    private val tilgangsstyringService: TilgangsstyringService,
    private val behandlingRepo: BehandlingRepo,
    private val clock: Clock,
    private val statistikkSakService: StatistikkSakService,
    private val statistikkSakRepo: StatistikkSakRepo,
    private val sessionFactory: SessionFactory,
) {
    /**
     * @throws [TilgangException] & [NullPointerException] dersom ikke tilgang eller behandling ikke eksisterer / feil id
     */
    suspend fun overta(command: OvertaBehandlingCommand): Either<KunneIkkeOvertaBehandling, Behandling> {
        val behandling = behandlingRepo.hent(command.behandlingId)
        tilgangsstyringService.krevTilgangTilPerson(command.saksbehandler, behandling.fnr, command.correlationId)

        return behandling.overta(command.saksbehandler, clock).onRight {
            when (it.status) {
                Behandlingsstatus.UNDER_BEHANDLING -> {
                    val statistikk = statistikkSakService.genererStatistikkForOppdatertSaksbehandlerEllerBeslutter(it)
                    sessionFactory.withTransactionContext { tx ->
                        behandlingRepo.overtaSaksbehandler(it.id, command.saksbehandler, command.overtarFra, tx)
                        statistikkSakRepo.lagre(statistikk, tx)
                    }
                }

                Behandlingsstatus.UNDER_BESLUTNING -> {
                    val statistikk = statistikkSakService.genererStatistikkForOppdatertSaksbehandlerEllerBeslutter(it)
                    sessionFactory.withTransactionContext { tx ->
                        behandlingRepo.overtaBeslutter(it.id, command.saksbehandler, command.overtarFra, tx)
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
        }
    }
}
