package no.nav.tiltakspenger.saksbehandling.domene.behandling

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler

data class StartRevurderingKommando(
    val sakId: SakId,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
)
