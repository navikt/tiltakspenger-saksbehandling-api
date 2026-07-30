package no.nav.tiltakspenger.saksbehandling.infra.metrikker

import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Utbetalingsstatus

/**
 * Varsler når et vedtaksbrev journalføres selv om utbetalingen har feilet mot oppdrag.
 *
 * Vi holder ikke brevet tilbake.
 * Vedtaket er fattet i det saksbehandler iverksetter, bruker har krav på å få det, og klagefristen løper.
 * En feil mot økonomisystemet er en driftssak som skal rettes, ikke noe som skal utsette et gyldig vedtak.
 * Men avviket må være synlig, for brevet forteller bruker om penger økonomisystemet har avvist.
 *
 * @param kontekst Identifiserende felter for vedtaket, brukt i loggmeldingen.
 */
fun varsleHvisUtbetalingHarFeilet(
    log: KLogger,
    utbetalingsstatus: Utbetalingsstatus?,
    kontekst: () -> String,
) {
    if (utbetalingsstatus != Utbetalingsstatus.FeiletMotOppdrag) {
        return
    }
    MetricRegister.VEDTAKSBREV_JOURNALFØRT_MED_FEILET_UTBETALING.inc()
    log.error { "Journalfører vedtaksbrev selv om utbetalingen har feilet mot oppdrag. Brevet forteller bruker om en utbetaling økonomisystemet har avvist. ${kontekst()}" }
}
