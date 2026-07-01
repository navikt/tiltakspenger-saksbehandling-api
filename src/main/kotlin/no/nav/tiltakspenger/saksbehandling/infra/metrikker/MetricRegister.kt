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
}
