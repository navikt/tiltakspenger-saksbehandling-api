package no.nav.tiltakspenger.saksbehandling.infra.metrikker

import io.micrometer.core.instrument.MeterRegistry

/** Beholder metrikknavnene som de eksisterende utbetalingsalarmene følger. */
class Utbetalingsmålinger(meterRegistry: MeterRegistry) {
    val feilet = meterRegistry.counter("tpts_saksbehandlingapi_utbetaling_feilet_count")
    val ikkeOk = meterRegistry.counter("tpts_saksbehandlingapi_utbetaling_ikke_ok_count")
}
