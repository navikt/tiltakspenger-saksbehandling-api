package no.nav.tiltakspenger.saksbehandling.klage.domene.forvaltningsmelding

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster

data class ForvaltningsmeldingKommando(
    val sakId: SakId,
    val klagebehandlingId: KlagebehandlingId,
    val saksbehandler: Saksbehandler,
    val correlationId: CorrelationId,
    val brevtekster: Brevtekster,
)
