package no.nav.tiltakspenger.saksbehandling.repository.utbetaling

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus

fun Utbetalingsstatus.toDbType(): String {
    return when (this) {
        Utbetalingsstatus.IkkePåbegynt -> "IKKE_PÅBEGYNT"
        Utbetalingsstatus.SendtTilOppdrag -> "SENDT_TIL_OPPDRAG"
        Utbetalingsstatus.FeiletMotOppdrag -> "FEILET_MOT_OPPDRAG"
        Utbetalingsstatus.Ok -> "OK"
        Utbetalingsstatus.OkUtenUtbetaling -> "OK_UTEN_UTBETALING"
    }
}

fun String?.toUtbetalingsstatus(): Utbetalingsstatus? {
    return when (this) {
        "IKKE_PÅBEGYNT" -> Utbetalingsstatus.IkkePåbegynt
        "SENDT_TIL_OPPDRAG" -> Utbetalingsstatus.SendtTilOppdrag
        "FEILET_MOT_OPPDRAG" -> Utbetalingsstatus.FeiletMotOppdrag
        "OK" -> Utbetalingsstatus.Ok
        "OK_UTEN_UTBETALING" -> Utbetalingsstatus.OkUtenUtbetaling
        null -> null
        else -> throw IllegalArgumentException("Ugyldig utbetalingsstatus: $this")
    }
}
