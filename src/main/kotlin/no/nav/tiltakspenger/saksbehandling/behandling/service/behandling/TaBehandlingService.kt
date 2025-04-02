package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.personklient.pdl.TilgangsstyringService
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.KanIkkeTaBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException

class TaBehandlingService(
    private val tilgangsstyringService: TilgangsstyringService,
    private val behandlingRepo: BehandlingRepo,
) {
    val logger = KotlinLogging.logger { }

    suspend fun taBehandling(
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
    ): Either<KanIkkeTaBehandling, Behandling> {
        val behandling = behandlingRepo.hent(behandlingId)
        tilgangsstyringService.harTilgangTilPerson(behandling.fnr, saksbehandler.roller, correlationId)
            .onLeft {
                throw TilgangException("Feil ved tilgangssjekk til person ved sending av behandling tilbake til saksbehandler. Feilen var $it")
            }.onRight {
                if (!it) throw TilgangException("Saksbehandler ${saksbehandler.navIdent} har ikke tilgang til person")
            }

        if (!saksbehandler.erSaksbehandlerEllerBeslutter()) {
            logger.warn { "Navident ${saksbehandler.navIdent} med rollene ${saksbehandler.roller} har ikke tilgang til å ta behandling" }
            return KanIkkeTaBehandling.MåVæreSaksbehandlerEllerBeslutter.left()
        }

        return behandling.taBehandling(saksbehandler).also {
            when (it.status) {
                Behandlingsstatus.UNDER_BEHANDLING -> behandlingRepo.taBehandlingSaksbehandler(
                    it.id,
                    saksbehandler,
                    it.status,
                )

                Behandlingsstatus.UNDER_BESLUTNING -> behandlingRepo.taBehandlingBeslutter(
                    it.id,
                    saksbehandler,
                    it.status,
                )

                Behandlingsstatus.KLAR_TIL_BESLUTNING,
                Behandlingsstatus.KLAR_TIL_BEHANDLING,
                Behandlingsstatus.VEDTATT,
                Behandlingsstatus.AVBRUTT,
                -> throw IllegalStateException("Behandlingen er i en ugyldig status for å kunne overta")
            }
        }.right()
    }
}
