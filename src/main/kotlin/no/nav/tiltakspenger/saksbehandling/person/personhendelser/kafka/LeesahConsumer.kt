package no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.tiltakspenger.libs.kafka.Consumer
import no.nav.tiltakspenger.libs.kafka.ManagedKafkaConsumer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.kafka.config.LocalKafkaConfig
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.PersonhendelseService
import org.apache.kafka.common.serialization.StringDeserializer

class LeesahConsumer(
    private val personhendelseService: PersonhendelseService,
    topic: String,
    groupId: String = KAFKA_CONSUMER_GROUP_ID,
    kafkaConfig: KafkaConfig = if (Configuration.isNais()) KafkaConfigImpl(autoOffsetReset = "none") else LocalKafkaConfig(),
) : Consumer<String, Personhendelse> {

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

    override suspend fun consume(key: String, value: Personhendelse) {
        when (value.opplysningstype) {
            Opplysningstype.DOEDSFALL_V1.name,
            Opplysningstype.FORELDERBARNRELASJON_V1.name,
            Opplysningstype.ADRESSEBESKYTTELSE_V1.name,
            -> personhendelseService.behandlePersonhendelse(value)
        }
    }

    override fun run() = consumer.run()
}
