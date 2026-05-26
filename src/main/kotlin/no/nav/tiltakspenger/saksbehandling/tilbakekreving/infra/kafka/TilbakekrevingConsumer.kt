package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.kafka.Consumer
import no.nav.tiltakspenger.libs.kafka.ManagedKafkaConsumer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.kafka.config.LocalKafkaConfig
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.tilNyTilbakekrevingshendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingHendelseRepo
import org.apache.kafka.common.serialization.StringDeserializer

private val logger = KotlinLogging.logger { }

class TilbakekrevingConsumer(
    private val tilbakekrevingHendelseRepo: TilbakekrevingHendelseRepo,
    topic: String,
    groupId: String = "$KAFKA_CONSUMER_GROUP_ID-v4",
    kafkaConfig: KafkaConfig = if (Configuration.isNais()) KafkaConfigImpl(autoOffsetReset = "earliest") else LocalKafkaConfig(),
    log: KLogger? = logger,
) : Consumer<String, String?> {

    private val consumer = ManagedKafkaConsumer(
        topic = topic,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = StringDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = groupId,
        ),
        log = log,
        consume = ::consume,
        kanLoggeKey = false,
    )

    override suspend fun consume(key: String, value: String?) {
        consume(key, value, tilbakekrevingHendelseRepo)
    }

    override fun run() = consumer.run()

    companion object {

        fun consume(
            key: String,
            value: String?,
            tilbakekrevingHendelseRepo: TilbakekrevingHendelseRepo,
        ) {
            // OBS: Merk at key er fødselsnummer, så det skal ikke logges.
            if (value == null) {
                logger.warn { "Mottatt tilbakekrevingshendelse uten value, hendelsen forkastes." }
                return
            }

            val hendelse = value.tilNyTilbakekrevingshendelse()

            if (hendelse == null) {
                logger.debug { "Mottatt tilbakekrevingshendelse som vi tp-sak har produsert, hendelsen forkastes." }
                return
            }

            val bleLagret = tilbakekrevingHendelseRepo.lagreNy(hendelse, key, value)

            if (!bleLagret) {
                logger.error { "Tilbakekrevingshendelse ble ikke lagret - ${hendelse.hendelsestype} / ${hendelse.eksternFagsakId} / ${hendelse.opprettet}" }
            } else {
                logger.info { "Lagret ny tilbakekrevingshendelse - type ${hendelse.hendelsestype}." }
            }
        }
    }
}
