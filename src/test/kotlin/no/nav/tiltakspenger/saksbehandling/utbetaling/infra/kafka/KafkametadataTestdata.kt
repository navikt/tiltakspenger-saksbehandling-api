package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka

import no.nav.tiltakspenger.saksbehandling.felles.Kafkametadata
import java.time.Instant

fun kafkametadata(offset: Long, partisjon: Int = 0) = Kafkametadata(
    topic = "helved.status.v1",
    partisjon = partisjon,
    offset = offset,
    tidspunkt = Instant.parse("2026-11-09T08:00:00Z"),
    tidspunktstype = "CreateTime",
    headere = mapOf("fagsystem" to "TILTAKSPENGER"),
)
