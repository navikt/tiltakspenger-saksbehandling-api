package no.nav.tiltakspenger.saksbehandling.service.behandling

import arrow.core.getOrElse
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.OppdaterBarnetilleggKommando
import no.nav.tiltakspenger.saksbehandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService

class OppdaterBarnetilleggService(
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
) {
    suspend fun oppdaterBarnetillegg(
        kommando: OppdaterBarnetilleggKommando,
    ): Behandling {
        // Denne sjekker tilgang til person og sak.
        val sak = sakService.hentForSakId(kommando.sakId, kommando.saksbehandler, kommando.correlationId).getOrElse {
            throw IllegalStateException("Kunne ikke oppdatere barnetillegg. Fant ikke sak. sakId=${kommando.sakId}, behandlingId=${kommando.behandlingId}")
        }
        val behandling = sak.hentBehandling(kommando.behandlingId)!!

        return behandling.oppdaterBarnetillegg(kommando).also {
            behandlingRepo.lagre(it)
        }
    }
}
