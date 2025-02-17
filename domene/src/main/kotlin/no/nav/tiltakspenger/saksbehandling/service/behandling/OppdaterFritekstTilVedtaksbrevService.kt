package no.nav.tiltakspenger.saksbehandling.service.behandling

import arrow.core.getOrElse
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.domene.behandling.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.ports.BehandlingRepo
import no.nav.tiltakspenger.saksbehandling.service.sak.SakService

class OppdaterFritekstTilVedtaksbrevService(
    private val sakService: SakService,
    private val behandlingRepo: BehandlingRepo,
) {
    suspend fun oppdaterFritekstTilVedtaksbrev(
        sakId: SakId,
        behandlingId: BehandlingId,
        saksbehandler: Saksbehandler,
        correlationId: CorrelationId,
        fritekstTilVedtaksbrev: FritekstTilVedtaksbrev,
    ): Behandling {
        // Denne sjekker tilgang til person og sak.
        val sak = sakService.hentForSakId(sakId, saksbehandler, correlationId).getOrElse {
            throw IllegalStateException("Kunne ikke oppdatere fritekst til vedtaksbrev. Fant ikke sak. sakId=$sakId, behandlingId=$behandlingId")
        }
        val behandling = sak.hentBehandling(behandlingId)!!

        return behandling.oppdaterFritekstTilVedtaksbrev(saksbehandler, fritekstTilVedtaksbrev).also {
            behandlingRepo.lagre(it)
        }
    }
}
