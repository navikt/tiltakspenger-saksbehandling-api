package no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.ServiceCommand
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import java.time.LocalDateTime

data class IverksettAvvisningKommando(
    val sakId: SakId,
    val klagebehandlingId: KlagebehandlingId,
    val iverksattTidspunkt: LocalDateTime,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
) : ServiceCommand
