package no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.tiltakspenger.libs.kafka.Consumer
import no.nav.tiltakspenger.libs.kafka.ManagedKafkaConsumer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.kafka.config.LocalKafkaConfig
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.IdenthendelseService
import org.apache.kafka.common.serialization.StringDeserializer

class AktorV2Consumer(
    private val identhendelseService: IdenthendelseService,
    topic: String,
    groupId: String = KAFKA_CONSUMER_GROUP_ID,
    kafkaConfig: KafkaConfig = if (Configuration.isNais()) KafkaConfigImpl(autoOffsetReset = "latest") else LocalKafkaConfig(),
) : Consumer<String, Aktor?> {
    private val consumer = ManagedKafkaConsumer(
        kanLoggeKey = false,
        topic = topic,
        config = kafkaConfig.avroConsumerConfig(
            keyDeserializer = StringDeserializer(),
            valueDeserializer = KafkaAvroDeserializer(),
            groupId = groupId,
            useSpecificAvroReader = true,
        ),
        consume = ::consume,
    )

    override suspend fun consume(key: String, value: Aktor?) {
        value?.let { identhendelseService.behandleIdenthendelse(it) }
    }

    override fun run() = consumer.run()
}
