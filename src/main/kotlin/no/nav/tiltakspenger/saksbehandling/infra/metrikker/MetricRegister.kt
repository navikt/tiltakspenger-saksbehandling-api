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

    val SOKNAD_BEHANDLET_DELVIS_AUTOMATISK: Counter = Counter.builder()
        .name("${METRICS_NS}_soknad_behandlet_delvis_automatisk_count")
        .help("Antall søknader som er behandlet delvis automatisk")
        .withoutExemplars()
        .register()

    val SOKNAD_IKKE_BEHANDLET_AUTOMATISK: Counter = Counter.builder()
        .name("${METRICS_NS}_soknad_ikke_behandlet_automatisk_count")
        .help("Antall søknader som ikke kunne behandles delvis automatisk")
        .withoutExemplars()
        .register()

    val SOKNAD_BEHANDLES_MANUELT_GRUNN: Counter = Counter.builder()
        .name("${METRICS_NS}_soknad_behandles_manuelt_grunn_count")
        .labelNames("grunn")
        .help("Grunner til at søknader må behandles manuelt")
        .withoutExemplars()
        .register()

    val SØKNAD_BEHANDLET_PÅ_NYTT: Counter = Counter.builder()
        .name("${METRICS_NS}_soknad_behandlet_paa_nytt_count")
        .help("Antall søknader som er behandlet på nytt")
        .withoutExemplars()
        .register()

    val IVERKSATT_BEHANDLING: Counter = Counter.builder()
        .name("${METRICS_NS}_iverksatt_behandling_count")
        .help("Antall iverksatte behandlinger")
        .withoutExemplars()
        .register()
}
