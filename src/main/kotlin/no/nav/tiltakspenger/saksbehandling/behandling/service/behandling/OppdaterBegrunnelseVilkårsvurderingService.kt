package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService

class OppdaterBegrunnelseVilkårsvurderingService(
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
) {
    suspend fun oppdaterBegrunnelseVilkårsvurdering(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
        begrunnelseVilkårsvurdering: BegrunnelseVilkårsvurdering,
    ): Behandling {
        // Denne sjekker tilgang til person og sak.
        val sak = sakService.sjekkTilgangOgHentForSakId(sakId, saksbehandler, correlationId)
        val behandling = sak.hentBehandling(behandlingId)!!
        // Denne validerer saksbehandler
        return behandling.oppdaterBegrunnelseVilkårsvurdering(saksbehandler, begrunnelseVilkårsvurdering).also {
            behandlingRepo.lagre(it)
        }
    }
}
