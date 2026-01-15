package no.nav.tiltakspenger.saksbehandling.klage.domene.brev

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId

/**
 * Brukes både til forhåndsvisning og lagring av tekster til brev for klagebehandling
 */
data class KlagebehandlingBrevKommando(
    val sakId: SakId,
    val klagebehandlingId: KlagebehandlingId,
    val saksbehandler: Saksbehandler,
    val correlationId: CorrelationId,
    val brevtekster: Brevtekster,
)
