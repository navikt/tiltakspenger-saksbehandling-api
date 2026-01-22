package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId

data class StartRevurderingKommando(
    val sakId: SakId,
    val correlationId: CorrelationId,
    val saksbehandler: Saksbehandler,
    val revurderingType: RevurderingType,
    val vedtakIdSomOmgjøres: VedtakId?,
    val klagebehandlingId: KlagebehandlingId?,
)
