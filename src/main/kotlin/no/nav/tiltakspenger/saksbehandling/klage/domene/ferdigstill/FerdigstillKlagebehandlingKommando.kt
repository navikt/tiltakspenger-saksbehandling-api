package no.nav.tiltakspenger.saksbehandling.klage.domene.ferdigstill

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId

data class FerdigstillKlagebehandlingKommando(
    val sakId: SakId,
    val klagebehandlingId: KlagebehandlingId,
    val saksbehandler: Saksbehandler,
    val begrunnelse: Begrunnelse?,
    val correlationId: CorrelationId,
)
