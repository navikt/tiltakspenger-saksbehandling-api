package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus

enum class UtbetalingsstatusDTO {
    /** Meldekortbehandlingen er ikke godkjent/iverksatt/vedtatt */
    IKKE_GODKJENT,

    /** Kan være fordi jobben ikke har kjørt enda, eller fordi forrige utbetaling ikke er OK, eller fordi helved ikke aksepterer utbetalingen. */
    IKKE_SENDT_TIL_HELVED,

    /** Vi har fått 202 Accepted fra helved. De vil prøve sende den til Oppdrag. Hvis Oppdrag er stengt kan det gå en stund til de mottar kvittering. Hos de vil denne da få statusen IKKE_PÅBEGYNT. */
    SENDT_TIL_HELVED,

    /** Helved har sendt utbetalingen til Oppdrag og venter på kvittering.Hvis Oppdrag er stengt kan det gå en stund til de mottar kvittering. */
    SENDT_TIL_OPPDRAG,

    /** Helved har identifisert at utbetalingen er lik siste overføring til Oppdrag og vil ikke sende noe. */
    OK_UTEN_UTBETALING,

    /** OK-kvittering fra Oppdrag */
    OK,

    /** Kvitteringen fra oppdrag hadde en feil-status. I disse tilfellene må sannsynligvis helved eller en utvikler i fagsystemet følge opp. */
    FEILET_MOT_OPPDRAG,

    AVBRUTT,
}

fun Utbetalingsstatus?.toUtbetalingsstatusDTO(): UtbetalingsstatusDTO {
    return when (this) {
        Utbetalingsstatus.IkkePåbegynt -> UtbetalingsstatusDTO.SENDT_TIL_HELVED
        Utbetalingsstatus.SendtTilOppdrag -> UtbetalingsstatusDTO.SENDT_TIL_OPPDRAG
        Utbetalingsstatus.FeiletMotOppdrag -> UtbetalingsstatusDTO.FEILET_MOT_OPPDRAG
        Utbetalingsstatus.Ok -> UtbetalingsstatusDTO.OK
        Utbetalingsstatus.OkUtenUtbetaling -> UtbetalingsstatusDTO.OK_UTEN_UTBETALING
        Utbetalingsstatus.Avbrutt -> UtbetalingsstatusDTO.AVBRUTT
        null -> UtbetalingsstatusDTO.IKKE_SENDT_TIL_HELVED
    }
}
