package no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.arena

import mu.KotlinLogging
import no.nav.tiltakspenger.libs.kafka.Consumer
import no.nav.tiltakspenger.libs.kafka.ManagedKafkaConsumer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.kafka.config.LocalKafkaConfig
import no.nav.tiltakspenger.vedtak.Configuration
import no.nav.tiltakspenger.vedtak.KAFKA_CONSUMER_GROUP_ID
import org.apache.kafka.common.serialization.StringDeserializer

class TiltaksdeltakerArenaConsumer(
    topic: String,
    groupId: String = KAFKA_CONSUMER_GROUP_ID,
    kafkaConfig: KafkaConfig = if (Configuration.isNais()) KafkaConfigImpl() else LocalKafkaConfig(),
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
        log.info { "Mottatt tiltaksdeltakelse fra arena med key $key" }
    }

    override fun run() = consumer.run()
}
