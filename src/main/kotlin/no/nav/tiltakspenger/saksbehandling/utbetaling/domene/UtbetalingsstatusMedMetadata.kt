package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk

data class UtbetalingsstatusMedMetadata(
    val status: Utbetalingsstatus,
    val metadata: Forsøkshistorikk,
)
