package no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster

/**
 * Kommando for å iverksette en klagebehandling.
 * Brukes til avvisning, opprettholdelse eller omgjøring etter klage.
 */
data class IverksettKlagebehandlingKommando(
    val sakId: SakId,
    val klagebehandlingId: KlagebehandlingId,
    val saksbehandler: Saksbehandler,
    val correlationId: CorrelationId,
)
