package no.nav.tiltakspenger.saksbehandling.klage.domene.overta

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId

data class OvertaKlagebehandlingKommando(
    val sakId: SakId,
    val klagebehandlingId: KlagebehandlingId,
    val saksbehandler: Saksbehandler,
    val overtarFra: String,
    val correlationId: CorrelationId,
)
