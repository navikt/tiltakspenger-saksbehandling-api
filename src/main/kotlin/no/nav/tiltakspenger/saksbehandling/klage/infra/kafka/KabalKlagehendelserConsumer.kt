package no.nav.tiltakspenger.saksbehandling.klage.infra.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.kafka.Consumer
import no.nav.tiltakspenger.libs.kafka.ManagedKafkaConsumer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.kafka.config.LocalKafkaConfig
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.KlagehendelseId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.NyKlagehendelse
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagehendelseRepo
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Clock
import java.time.LocalDateTime

class KabalKlagehendelserConsumer(
    private val klagehendelseRepo: KlagehendelseRepo,
    topic: String = "klage.behandling-events.v1",
    groupId: String = KAFKA_CONSUMER_GROUP_ID,
    kafkaConfig: KafkaConfig = if (Configuration.isNais()) KafkaConfigImpl(autoOffsetReset = "earliest") else LocalKafkaConfig(),
    private val clock: Clock,
) : Consumer<String, String?> {
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
        consume(key, value, clock, klagehendelseRepo::lagreNyHendelse)
    }

    companion object {
        private val log = KotlinLogging.logger { }

        /**
         * Trukket ut i companion object for å kunne testes isolert uten å måtte starte en KafkaConsumer, som er tungvint å teste mot.
         */
        fun consume(
            key: String,
            value: String?,
            clock: Clock,
            lagreNyHendelse: (NyKlagehendelse) -> Unit,
        ): KlagehendelseId? {
            if (value == null) {
                log.warn { "Mottatt klagehendelse fra Kabal med key $key - Ignorerer tombstonet hendelse med id $key" }
                return null
            }
            val enkelKabalKlagehendelseDTO = value.tilEnkelKabalKlagehendelseDTO()
            if (!enkelKabalKlagehendelseDTO.erAktuell) {
                log.debug { "Mottatt klagehendelse fra Kabal - Ignorerer uaktuell klagehendelse (kilde matcher ikke ${enkelKabalKlagehendelseDTO.aktuellKilde}): $enkelKabalKlagehendelseDTO" }
                return null
            }
            log.info { "Mottatt klagehendelse fra Kabal med key $key - lagrer hendelse: $enkelKabalKlagehendelseDTO" }
            val nyKlagehendelse = NyKlagehendelse(
                eksternKlagehendelseId = enkelKabalKlagehendelseDTO.eventId,
                opprettet = LocalDateTime.now(clock),
                key = key,
                value = objectMapper.writeValueAsString(objectMapper.readTree(value)),
            )
            lagreNyHendelse(nyKlagehendelse)
            return nyKlagehendelse.klagehendelseId
        }
    }

    override fun run() = consumer.run()
}
