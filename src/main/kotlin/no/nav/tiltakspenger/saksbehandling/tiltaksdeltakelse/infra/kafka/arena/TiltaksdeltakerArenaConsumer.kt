package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.arena

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.kafka.infra.Consumer
import no.nav.tiltakspenger.libs.kafka.infra.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.infra.ManagedKafkaConsumer
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.TiltaksdeltakerService
import org.apache.kafka.common.serialization.StringDeserializer

class TiltaksdeltakerArenaConsumer(
    private val tiltaksdeltakerService: TiltaksdeltakerService,
    topic: String,
    groupId: String = KAFKA_CONSUMER_GROUP_ID,
    kafkaConfig: KafkaConfig = if (Configuration.isNais()) KafkaConfig.fraNaisEnv(autoOffsetReset = "none") else KafkaConfig(kafkaBrokers = "localhost:9092"),
    private val log: KLogger? = KotlinLogging.logger {},
) : Consumer<String, String> {

    private val consumer = ManagedKafkaConsumer(
        topic = topic,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = StringDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = groupId,
        ),
        log = log,
        consume = ::consume,
    )

    override suspend fun consume(key: String, value: String) {
        log?.info { "Mottatt tiltaksdeltakelse fra arena med key $key" }
        tiltaksdeltakerService.behandleMottattArenadeltaker(deltakerId = key, melding = value)
    }

    override fun run() = consumer.run()
}
