package no.nav.tiltakspenger.saksbehandling.klage.domene.avbryt

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId

data class AvbrytKlagebehandlingKommando(
    val sakId: SakId,
    val klagebehandlingId: KlagebehandlingId,
    val begrunnelse: NonBlankString,
    val saksbehandler: Saksbehandler,
    val correlationId: CorrelationId,
)
