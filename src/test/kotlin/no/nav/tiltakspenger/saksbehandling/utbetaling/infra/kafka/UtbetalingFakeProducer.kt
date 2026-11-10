package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingProducer
import java.util.concurrent.ConcurrentHashMap

class UtbetalingFakeProducer : UtbetalingProducer {
    private val data = ConcurrentHashMap<UtbetalingId, String>()

    val produserteUtbetalinger: Map<UtbetalingId, String> get() = data.toMap()

    override fun produser(utbetalingId: UtbetalingId, helvedUtbetalingJson: String) {
        data[utbetalingId] = helvedUtbetalingJson
    }
}
