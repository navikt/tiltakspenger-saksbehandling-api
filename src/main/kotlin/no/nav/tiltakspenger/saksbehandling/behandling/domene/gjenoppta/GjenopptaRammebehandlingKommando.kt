package no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenoppta

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler

data class GjenopptaRammebehandlingKommando(
    val sakId: SakId,
    val rammebehandlingId: BehandlingId,
    val saksbehandler: Saksbehandler,
    val correlationId: CorrelationId,
)
