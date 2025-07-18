package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler

data class StartRevurderingKommando(
    val sakId: SakId,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
    val revurderingType: RevurderingType,
)
