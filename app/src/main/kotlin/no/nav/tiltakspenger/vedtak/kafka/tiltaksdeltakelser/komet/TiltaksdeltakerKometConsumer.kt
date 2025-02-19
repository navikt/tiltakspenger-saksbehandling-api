package no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.komet

import mu.KotlinLogging
import no.nav.tiltakspenger.libs.kafka.Consumer
import no.nav.tiltakspenger.libs.kafka.ManagedKafkaConsumer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.kafka.config.LocalKafkaConfig
import no.nav.tiltakspenger.vedtak.Configuration
import no.nav.tiltakspenger.vedtak.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.TiltaksdeltakerService
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import java.util.UUID

class TiltaksdeltakerKometConsumer(
    private val tiltaksdeltakerService: TiltaksdeltakerService,
    topic: String,
    groupId: String = KAFKA_CONSUMER_GROUP_ID,
    kafkaConfig: KafkaConfig = if (Configuration.isNais()) KafkaConfigImpl(autoOffsetReset = "latest") else LocalKafkaConfig(),
) : Consumer<UUID, String?> {
    private val log = KotlinLogging.logger { }

    private val consumer = ManagedKafkaConsumer(
        topic = topic,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = groupId,
        ),
        consume = ::consume,
    )

    override suspend fun consume(key: UUID, value: String?) {
        log.info { "Mottatt tiltaksdeltakelse fra komet med key $key" }
        if (value == null) {
            log.warn { "Ignorerer tombstonet deltaker med id $key" }
            return
        }
        tiltaksdeltakerService.behandleMottattKometdeltaker(deltakerId = key, melding = value)
    }

    override fun run() = consumer.run()
}
