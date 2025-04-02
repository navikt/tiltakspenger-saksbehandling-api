package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.overta

import arrow.core.Either
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException
import java.time.Clock

class OvertaBehandlingService(
    private val tilgangsstyringService: TilgangsstyringService,
    private val behandlingRepo: BehandlingRepo,
    private val clock: Clock,
) {
    /**
     * @throws [TilgangException] & [NullPointerException] dersom ikke tilgang eller behandling ikke eksisterer / feil id
     */
    suspend fun overta(command: OvertaBehandlingCommand): Either<KunneIkkeOvertaBehandling, Behandling> {
        val behandling = behandlingRepo.hent(command.behandlingId)
        tilgangsstyringService.harTilgangTilPerson(behandling.fnr, command.saksbehandler.roller, command.correlationId)
            .onLeft {
                throw TilgangException("Feil ved tilgangssjekk til person ved sending av behandling tilbake til saksbehandler. Feilen var $it")
            }.onRight {
                if (!it) throw TilgangException("Saksbehandler ${command.saksbehandler.navIdent} har ikke tilgang til person")
            }

        return behandling.overta(command.saksbehandler, clock).onRight {
            when (it.status) {
                Behandlingsstatus.UNDER_BEHANDLING -> behandlingRepo.overtaSaksbehandler(it.id, command.saksbehandler, command.overtarFra)
                Behandlingsstatus.UNDER_BESLUTNING -> behandlingRepo.overtaBeslutter(it.id, command.saksbehandler, command.overtarFra)
                Behandlingsstatus.KLAR_TIL_BESLUTNING,
                Behandlingsstatus.KLAR_TIL_BEHANDLING,
                Behandlingsstatus.VEDTATT,
                Behandlingsstatus.AVBRUTT,
                -> throw IllegalStateException("Behandlingen er i en ugyldig status for å kunne overta")
            }
        }
    }
}
