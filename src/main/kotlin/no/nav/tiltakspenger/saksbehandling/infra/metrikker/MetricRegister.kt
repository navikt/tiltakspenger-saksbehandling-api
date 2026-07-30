package no.nav.tiltakspenger.saksbehandling.infra.metrikker

import io.prometheus.metrics.core.metrics.Counter

private const val METRICS_NS = "tpts_saksbehandlingapi"

object MetricRegister {
    val UTBETALING_FEILET: Counter = Counter.builder()
        .name("${METRICS_NS}_utbetaling_feilet_count")
        .help("Antall feilede utbetalinger")
        .withoutExemplars()
        .register()

    val UTBETALING_IKKE_OK: Counter = Counter.builder()
        .name("${METRICS_NS}_utbetaling_ikke_ok_count")
        .help("Antall utbetalinger som ikke har fått ok-status etter tre dager")
        .withoutExemplars()
        .register()

    /**
     * Vedtaket er fattet ved iverksettelse, så vi holder ikke brevet tilbake når utbetalingen har feilet.
     * Bruker har krav på vedtaket sitt, og klagefristen løper.
     * Men avviket skal være synlig: brevet forteller om penger økonomisystemet har avvist.
     */
    val VEDTAKSBREV_JOURNALFØRT_MED_FEILET_UTBETALING: Counter = Counter.builder()
        .name("${METRICS_NS}_vedtaksbrev_journalfort_med_feilet_utbetaling_count")
        .help("Antall vedtaksbrev som ble journalført mens utbetalingen sto i FeiletMotOppdrag")
        .withoutExemplars()
        .register()
}
