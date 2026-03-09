package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.kafka.Consumer
import no.nav.tiltakspenger.libs.kafka.ManagedKafkaConsumer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.kafka.config.LocalKafkaConfig
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.record.tilNyTilbakekrevingshendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingHendelsePostgresRepo
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Clock

class TilbakekrevingConsumer(
    private val tilbakekrevingHendelseRepo: TilbakekrevingHendelsePostgresRepo,
    private val clock: Clock,
    topic: String,
    groupId: String = "$KAFKA_CONSUMER_GROUP_ID-v2",
    kafkaConfig: KafkaConfig = if (Configuration.isNais()) KafkaConfigImpl(autoOffsetReset = "earliest") else LocalKafkaConfig(),
) : Consumer<String, String?> {
    private val logger = KotlinLogging.logger { }

    private val consumer = ManagedKafkaConsumer(
        topic = topic,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = StringDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = groupId,
        ),
        consume = ::consume,
    )

    override suspend fun consume(key: String, value: String?) {
        logger.info { "Mottatt tilbakekrevingshendelse med key $key" }

        if (value == null) {
            logger.warn { "Mottatt tilbakekrevingshendelse med key $key uten value" }
            return
        }

        val hendelse = value.tilNyTilbakekrevingshendelse(key, clock).getOrElse {
            logger.error(it) { "Mottatt tilbakekrevingshendelse med key $key - Deserialize feilet for value $value" }
            throw it
        }

        logger.info { "Lagrer tilbakekrevingshendelse type ${hendelse.hendelsestype} med key $key" }

        tilbakekrevingHendelseRepo.lagreNy(hendelse, key, value)
    }

    override fun run() = consumer.run()
}
