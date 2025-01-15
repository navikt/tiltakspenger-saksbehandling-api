package no.nav.tiltakspenger.saksbehandling.domene.behandling

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler

data class OppdaterTilleggstekstBrevKommando(
    val behandlingId: BehandlingId,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
    val subsumsjon: TilleggstekstBrev.Subsumsjon,
)
