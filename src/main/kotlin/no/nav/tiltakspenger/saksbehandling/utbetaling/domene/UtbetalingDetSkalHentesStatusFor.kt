package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDateTime

data class UtbetalingDetSkalHentesStatusFor(
    val utbetalingId: UtbetalingId,
    val sakId: SakId,
    val saksnummer: Saksnummer,
    val opprettet: LocalDateTime,
    val sendtTilUtbetalingstidspunkt: LocalDateTime,
    val forsøkshistorikk: Forsøkshistorikk,
)
