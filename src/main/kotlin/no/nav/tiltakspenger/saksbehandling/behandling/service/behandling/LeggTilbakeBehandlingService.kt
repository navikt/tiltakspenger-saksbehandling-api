package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeLeggeTilbakeBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException

class LeggTilbakeBehandlingService(
    private val tilgangsstyringService: TilgangsstyringService,
    private val behandlingRepo: BehandlingRepo,
) {
    val logger = KotlinLogging.logger { }

    suspend fun leggTilbakeBehandling(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeLeggeTilbakeBehandling, Behandling> {
        val behandling = behandlingRepo.hent(behandlingId)
        tilgangsstyringService.harTilgangTilPerson(behandling.fnr, saksbehandler.roller, correlationId)
            .onLeft {
                throw TilgangException("Feil ved tilgangssjekk til person når saksbehandler legger tilbake behandling. Feilen var $it")
            }.onRight {
                if (!it) throw TilgangException("Saksbehandler ${saksbehandler.navIdent} har ikke tilgang til person")
            }

        if (!saksbehandler.erSaksbehandlerEllerBeslutter()) {
            logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å legge tilbake behandling" }
            return KanIkkeLeggeTilbakeBehandling.MåVæreSaksbehandlerEllerBeslutter.left()
        }

        return behandling.leggTilbakeBehandling(saksbehandler).onRight {
            when (it.status) {
                Behandlingsstatus.KLAR_TIL_BEHANDLING -> behandlingRepo.leggTilbakeBehandlingSaksbehandler(
                    it.id,
                    saksbehandler,
                    it.status,
                )

                Behandlingsstatus.KLAR_TIL_BESLUTNING -> behandlingRepo.leggTilbakeBehandlingBeslutter(
                    it.id,
                    saksbehandler,
                    it.status,
                )

                Behandlingsstatus.UNDER_BEHANDLING,
                Behandlingsstatus.UNDER_BESLUTNING,
                Behandlingsstatus.VEDTATT,
                Behandlingsstatus.AVBRUTT,
                -> throw IllegalStateException("Behandlingen er i en ugyldig status for å kunne legge tilbake")
            }
        }
    }
}
