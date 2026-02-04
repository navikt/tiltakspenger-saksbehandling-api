package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.engangsjobb

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.kafka.Consumer
import no.nav.tiltakspenger.libs.kafka.ManagedKafkaConsumer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.kafka.config.LocalKafkaConfig
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.TiltaksdeltakerService
import org.apache.kafka.common.serialization.StringDeserializer

class EngangsjobbArenaConsumer(
    private val tiltaksdeltakerService: TiltaksdeltakerService,
    topic: String,
    groupId: String = "tiltakspenger-saksbehandling-api-consumer-engangsjobb",
    kafkaConfig: KafkaConfig = if (Configuration.isNais()) KafkaConfigImpl(autoOffsetReset = "earliest") else LocalKafkaConfig(),
) : Consumer<String, String> {
    private val log = KotlinLogging.logger { }

    private val consumer = ManagedKafkaConsumer(
        topic = topic,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = StringDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = groupId,
        ),
        consume = ::consume,
    )

    override suspend fun consume(key: String, value: String) {
        log.info { "Lest tiltaksdeltakelse fra arena med key $key for engangsjobb" }
        tiltaksdeltakerService.behandleArenadeltakerForEngangsjobb(deltakerId = key, melding = value)
    }

    override fun run() = consumer.run()
}
