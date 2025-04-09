package no.nav.tiltakspenger.saksbehandling.infra.metrikker

import io.prometheus.metrics.core.metrics.Counter

const val METRICS_NS = "tpts_saksbehandlingapi"

object MetricRegister {
    val MOTTATT_SOKNAD: Counter = Counter.builder()
        .name("${METRICS_NS}_mottatt_soknad_count")
        .help("Antall mottatte søknader")
        .withoutExemplars()
        .register()

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

    val STARTET_BEHANDLING: Counter = Counter.builder()
        .name("${METRICS_NS}_startet_behandling_count")
        .help("Antall startede behandlinger")
        .withoutExemplars()
        .register()

    val IVERKSATT_BEHANDLING: Counter = Counter.builder()
        .name("${METRICS_NS}_iverksatt_behandling_count")
        .help("Antall iverksatte behandlinger")
        .withoutExemplars()
        .register()
}
