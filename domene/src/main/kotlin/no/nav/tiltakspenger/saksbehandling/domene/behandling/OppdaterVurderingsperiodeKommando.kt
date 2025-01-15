package no.nav.tiltakspenger.saksbehandling.domene.behandling

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode

data class OppdaterVurderingsperiodeKommando(
    val behandlingId: BehandlingId,
    val periode: Periode,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
)
