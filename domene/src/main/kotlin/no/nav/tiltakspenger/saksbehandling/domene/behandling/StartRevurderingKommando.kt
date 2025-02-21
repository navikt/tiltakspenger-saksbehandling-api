package no.nav.tiltakspenger.saksbehandling.domene.behandling

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import java.time.LocalDate

data class StartRevurderingKommando(
    val sakId: SakId,
    val fraOgMed: LocalDate,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
)

data class StartRevurderingV2Kommando(
    val sakId: SakId,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
)
