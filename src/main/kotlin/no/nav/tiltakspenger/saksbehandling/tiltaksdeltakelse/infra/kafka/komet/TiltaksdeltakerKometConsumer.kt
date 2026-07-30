package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.komet

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.kafka.infra.Consumer
import no.nav.tiltakspenger.libs.kafka.infra.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.infra.ManagedKafkaConsumer
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.TiltaksdeltakerService
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import java.util.UUID

class TiltaksdeltakerKometConsumer(
    private val tiltaksdeltakerService: TiltaksdeltakerService,
    topic: String,
    groupId: String = KAFKA_CONSUMER_GROUP_ID,
    kafkaConfig: KafkaConfig = if (Configuration.isNais()) KafkaConfig.fraNaisEnv(autoOffsetReset = "none") else KafkaConfig(kafkaBrokers = "localhost:9092"),
    private val log: KLogger? = KotlinLogging.logger {},
) : Consumer<UUID, String?> {

    private val consumer = ManagedKafkaConsumer(
        topic = topic,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = groupId,
        ),
        log = log,
        consume = ::consume,
    )

    override suspend fun consume(key: UUID, value: String?) {
        log?.info { "Mottatt tiltaksdeltakelse fra komet med key $key" }
        if (value == null) {
            log?.warn { "Ignorerer tombstonet deltaker med id $key" }
            return
        }
        tiltaksdeltakerService.behandleMottattKometdeltaker(deltakerId = key, melding = value)
    }

    override fun run() = consumer.run()
}
