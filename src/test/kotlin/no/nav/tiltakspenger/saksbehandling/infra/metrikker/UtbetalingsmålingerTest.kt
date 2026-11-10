package no.nav.tiltakspenger.saksbehandling.infra.metrikker

import io.kotest.matchers.shouldBe
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.junit.jupiter.api.Test

class UtbetalingsmålingerTest {
    @Test
    fun `eksporterer de samme navnene som utbetalingsalarmene følger`() {
        val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        val målinger = Utbetalingsmålinger(registry)
        målinger.feilet.increment()
        målinger.ikkeOk.increment()
        val eksport = registry.scrape()
        eksport.contains("tpts_saksbehandlingapi_utbetaling_feilet_count_total 1.0") shouldBe true
        eksport.contains("tpts_saksbehandlingapi_utbetaling_ikke_ok_count_total 1.0") shouldBe true
        registry.close()
    }
}
