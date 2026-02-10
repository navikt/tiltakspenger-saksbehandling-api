package no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import java.time.LocalDateTime

data class IverksettOmgjøringKommando(
    val sakId: SakId,
    val klagebehandlingId: KlagebehandlingId,
    val iverksattTidspunkt: LocalDateTime,
    val correlationId: CorrelationId,
)
