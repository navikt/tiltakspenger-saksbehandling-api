package no.nav.tiltakspenger.saksbehandling.infra.kafka

import no.nav.tiltakspenger.saksbehandling.felles.Kafkametadata
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.Instant

fun ConsumerRecord<*, *>.tilKafkametadata(): Kafkametadata = Kafkametadata(
    topic = topic(),
    partisjon = partition(),
    offset = offset(),
    tidspunkt = Instant.ofEpochMilli(timestamp()),
    tidspunktstype = timestampType().name,
    headere = headers().associate { it.key() to it.value()?.decodeToString() },
)
