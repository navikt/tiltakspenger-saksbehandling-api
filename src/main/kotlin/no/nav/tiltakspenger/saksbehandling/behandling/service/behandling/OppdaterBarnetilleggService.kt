package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterBarnetilleggKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo

class OppdaterBarnetilleggService(
    private val sakService: no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService,
    private val behandlingRepo: BehandlingRepo,
) {
    suspend fun oppdaterBarnetillegg(
        kommando: OppdaterBarnetilleggKommando,
    ): Behandling {
        // Denne sjekker tilgang til person og sak.
        val sak = sakService.sjekkTilgangOgHentForSakId(kommando.sakId, kommando.saksbehandler, kommando.correlationId)
        val behandling = sak.hentBehandling(kommando.behandlingId)!!

        require(behandling is Søknadsbehandling)
        // Denne validerer saksbehandler
        return behandling.oppdaterBarnetillegg(kommando).also {
            behandlingRepo.lagre(it)
        }
    }
}
