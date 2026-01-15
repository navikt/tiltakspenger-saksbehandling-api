package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler

data class ForhåndsvisBrevKlagebehandlingKommando(
    val sakId: SakId,
    val klagebehandlingId: KlagebehandlingId,
    val saksbehandler: Saksbehandler,
    val correlationId: CorrelationId,
    val tekstTilVedtaksbrev: List<TittelOgTekst>,
)
