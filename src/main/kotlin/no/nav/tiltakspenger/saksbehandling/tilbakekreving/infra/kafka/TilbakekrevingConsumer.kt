package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.kafka.infra.Consumer
import no.nav.tiltakspenger.libs.kafka.infra.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.infra.ManagedKafkaConsumer
import no.nav.tiltakspenger.saksbehandling.infra.setup.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.tilNyTilbakekrevingshendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.ports.TilbakekrevingHendelseRepo
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Clock

private val logger = KotlinLogging.logger { }

class TilbakekrevingConsumer(
    private val tilbakekrevingHendelseRepo: TilbakekrevingHendelseRepo,
    private val clock: Clock,
    private val erDev: Boolean,
    topic: String,
    groupId: String = "$KAFKA_CONSUMER_GROUP_ID-v4",
    kafkaConfig: KafkaConfig,
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
        consume(key, value, tilbakekrevingHendelseRepo, clock, erDev)
    }

    override fun run() = consumer.run()

    companion object {

        /**
         * @param erDev styrer kun loggnivået ved deserialiseringsfeil, og injiseres fra komposisjonsroten.
         *
         * @return id-en til hendelsen dersom den ble lagret, ellers null.
         */
        fun consume(
            key: String,
            value: String?,
            tilbakekrevingHendelseRepo: TilbakekrevingHendelseRepo,
            clock: Clock,
            erDev: Boolean,
        ): TilbakekrevinghendelseId? {
            // OBS: Merk at key er fødselsnummer, så det skal ikke logges.
            if (value == null) {
                logger.warn { "Mottatt tilbakekrevingshendelse uten value, hendelsen forkastes." }
                return null
            }

            val hendelse = value.tilNyTilbakekrevingshendelse(clock, erDev)

            if (hendelse == null) {
                logger.debug { "Mottatt tilbakekrevingshendelse som vi tp-sak har produsert, hendelsen forkastes." }
                return null
            }

            val bleLagret = tilbakekrevingHendelseRepo.lagreNy(hendelse, key, value)

            if (!bleLagret) {
                logger.error { "Tilbakekrevingshendelse ble ikke lagret - ${hendelse.hendelsestype} / ${hendelse.eksternFagsakId} / ${hendelse.opprettet}" }
                return null
            }

            logger.info { "Lagret ny tilbakekrevingshendelse - type ${hendelse.hendelsestype}." }
            return hendelse.id
        }
    }
}
