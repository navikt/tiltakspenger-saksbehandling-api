package no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.tiltakspenger.libs.kafka.avro.infra.AvroKafkaConfig
import no.nav.tiltakspenger.libs.kafka.infra.Consumer
import no.nav.tiltakspenger.libs.kafka.infra.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.infra.ManagedKafkaConsumer
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.IdenthendelseService
import org.apache.kafka.common.serialization.StringDeserializer

/**
 * Konsument for pdl.aktor-v2 (PDL identhendelser).
 * Dokumentasjon: https://pdl-docs.ansatt.nav.no/ekstern/index.html#identitetshendelser_pa_kafka
 * Skjema master: https://github.com/navikt/pdl/blob/master/libs/contract-pdl-avro/src/main/avro/no/nav/person/pdl/aktor/AktorV2.avdl
 * Skjema kopi internt: src/main/avro/AktorV2.avdl
 * Se [no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.LeesahConsumer] for andre PDLhendelser.
 */
class AktorV2Consumer(
    private val identhendelseService: IdenthendelseService,
    topic: String,
    groupId: String = KAFKA_CONSUMER_GROUP_ID,
    avroKafkaConfig: AvroKafkaConfig = if (Configuration.isNais()) {
        AvroKafkaConfig.fraNaisEnv(autoOffsetReset = "none")
    } else {
        AvroKafkaConfig(kafkaConfig = KafkaConfig(kafkaBrokers = "localhost:9092"), schemaRegistryUrl = "mock://test")
    },
    log: KLogger? = KotlinLogging.logger {},
) : Consumer<String, Aktor?> {
    private val consumer = ManagedKafkaConsumer(
        kanLoggeKey = false,
        topic = topic,
        config = avroKafkaConfig.avroConsumerConfig(
            keyDeserializer = StringDeserializer(),
            valueDeserializer = KafkaAvroDeserializer(),
            groupId = groupId,
        ),
        log = log,
        consume = ::consume,
    )

    override suspend fun consume(key: String, value: Aktor?) {
        value?.let { identhendelseService.behandleIdenthendelse(it) }
    }

    override fun run() = consumer.run()
}
