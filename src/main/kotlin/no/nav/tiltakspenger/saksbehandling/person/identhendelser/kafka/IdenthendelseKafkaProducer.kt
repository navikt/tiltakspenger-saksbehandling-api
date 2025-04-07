package no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.kafka.Producer
import java.util.UUID

class IdenthendelseKafkaProducer(
    private val kafkaProducer: Producer<String, String>,
    private val topic: String,
) {
    private val log = KotlinLogging.logger {}

    fun produserIdenthendelse(id: UUID, identhendelseDto: IdenthendelseDto) {
        kafkaProducer.produce(topic, id.toString(), objectMapper.writeValueAsString(identhendelseDto))
        log.info { "Produserte identhendelse med id $id til topic" }
    }
}

data class IdenthendelseDto(
    val gammeltFnr: String,
    val nyttFnr: String,
)
