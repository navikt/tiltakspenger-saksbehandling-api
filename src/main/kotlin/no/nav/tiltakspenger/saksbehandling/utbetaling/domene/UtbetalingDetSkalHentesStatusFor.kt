package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

data class UtbetalingDetSkalHentesStatusFor(
    val sakId: SakId,
    val vedtakId: VedtakId,
    val saksnummer: Saksnummer,
)
