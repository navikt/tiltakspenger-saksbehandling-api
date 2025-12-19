package no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.brev

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev

sealed interface ForhåndsvisVedtaksbrevKommando {
    val sakId: SakId
    val behandlingId: BehandlingId
    val correlationId: CorrelationId
    val saksbehandler: Saksbehandler
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?
}
