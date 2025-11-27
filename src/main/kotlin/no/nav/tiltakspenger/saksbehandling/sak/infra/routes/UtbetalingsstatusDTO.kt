package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

/**
 * Se også [no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus]
 */
enum class UtbetalingsstatusDTO {
    IkkePåbegynt,
    SendtTilOppdrag,
    FeiletMotOppdrag,
    Ok,
    OkUtenUtbetaling,
    Avbrutt,
}

fun no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus.toDTO(): UtbetalingsstatusDTO {
    return when (this) {
        no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus.IkkePåbegynt -> UtbetalingsstatusDTO.IkkePåbegynt
        no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus.SendtTilOppdrag -> UtbetalingsstatusDTO.SendtTilOppdrag
        no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus.FeiletMotOppdrag -> UtbetalingsstatusDTO.FeiletMotOppdrag
        no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus.Ok -> UtbetalingsstatusDTO.Ok
        no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus.OkUtenUtbetaling -> UtbetalingsstatusDTO.OkUtenUtbetaling
        no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus.Avbrutt -> UtbetalingsstatusDTO.Avbrutt
    }
}
