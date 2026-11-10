package no.nav.tiltakspenger.saksbehandling.infra.metrikker

import io.prometheus.metrics.core.metrics.Counter

private const val METRICS_NS = "tpts_saksbehandlingapi"

object MetricRegister {
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

    /**
     * Reversert opphør innvilger perioder uten gyldig tiltaksdeltakelse i saksopplysningene, så hvert tilfelle skal være synlig.
     * Se [varsleHvisReversertOpphørErIverksatt].
     */
    val REVERSERT_OPPHØR_IVERKSATT: Counter = Counter.builder()
        .name("${METRICS_NS}_reversert_opphor_iverksatt_count")
        .help("Antall iverksatte rammevedtak med reversert opphør")
        .withoutExemplars()
        .register()

    /**
     * Tilgangsmaskinen avviste med en avvisningskode vi ikke kjenner; koden er label.
     * Alarmen i `.nais/alerts.yml` står til koden er lagt til i `tilTilgangsvurderingAvvistÅrsak` og appen er deployet på nytt.
     */
    val TILGANGSMASKIN_UKJENT_AVVISNINGSKODE: Counter = Counter.builder()
        .name("${METRICS_NS}_tilgangsmaskin_ukjent_avvisningskode_count")
        .help("Antall avvisninger fra Tilgangsmaskinen med en avvisningskode vi ikke kjenner, per kode")
        .labelNames("kode")
        .withoutExemplars()
        .register()
}
