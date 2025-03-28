package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import arrow.core.getOrElse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterBarnetilleggKommando
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo

class OppdaterBarnetilleggService(
    private val sakService: no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService,
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
