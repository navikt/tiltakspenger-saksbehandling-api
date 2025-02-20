package no.nav.tiltakspenger.saksbehandling.service.behandling.brev

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.domene.behandling.FritekstTilVedtaksbrev

data class ForhåndsvisVedtaksbrevKommando(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
)
