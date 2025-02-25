package no.nav.tiltakspenger.saksbehandling.domene.behandling

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler

data class OppdaterBarnetilleggKommando(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val barnetillegg: no.nav.tiltakspenger.barnetillegg.Barnetillegg,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
)
