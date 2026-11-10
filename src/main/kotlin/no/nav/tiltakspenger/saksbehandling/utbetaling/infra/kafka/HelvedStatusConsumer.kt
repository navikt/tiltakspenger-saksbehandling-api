package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.kafka.infra.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.infra.ManagedKafkaConsumer
import no.nav.tiltakspenger.saksbehandling.felles.Kafkametadata
import no.nav.tiltakspenger.saksbehandling.infra.kafka.tilKafkametadata
import no.nav.tiltakspenger.saksbehandling.infra.setup.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingskvitteringRepo
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Clock
import java.time.LocalDateTime

/**
 * Lagrer statusmeldingene fra Hel Ved for tiltakspenger rått, med Kafka-metadata som hindrer duplikater, og lar [no.nav.tiltakspenger.saksbehandling.utbetaling.service.BehandleUtbetalingskvitteringerJobb] behandle dem.
 * Topicet deles av alle fagsystemene til Hel Ved, som setter headeren `fagsystem` på hver melding.
 * Kaster ved feil, slik at meldingen leses på nytt; en melding som aldri kan lagres, stopper consumeren.
 *
 * Kildekode: https://github.com/navikt/helved-utbetaling/blob/main/apps/urskog/main/urskog/Kafka.kt
 * Dokumentasjon: https://helved-docs.intern.dev.nav.no/kom_i_gang/sjekk_status_pa_utbetaling
 * API-spec (AsyncAPI): https://github.com/navikt/helved-docs/blob/main/public/asyncapi-spec.yml
 * Meldingsmodell: https://github.com/navikt/helved-utbetaling/blob/main/models/main/models/Status.kt
 * Slack: [#team-hel-ved](https://nav-it.slack.com/archives/C06SJTR2X3L)
 * Teamkatalog: https://teamkatalogen.nav.no/team/93e78ae9-babc-452c-965d-e7dc67af8612
 */
class HelvedStatusConsumer(
    utbetalingskvitteringRepo: UtbetalingskvitteringRepo,
    topic: String,
    groupId: String = KAFKA_CONSUMER_GROUP_ID,
    kafkaConfig: KafkaConfig,
    clock: Clock,
    meterRegistry: MeterRegistry,
    log: KLogger? = KotlinLogging.logger {},
) {
    private val consumer = ManagedKafkaConsumer<String?, String?>(
        topic = topic,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = StringDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = groupId,
        ),
        log = log,
        consumeRecord = { consume(it, clock, utbetalingskvitteringRepo::lagreNy) },
        clock = clock,
        meterRegistry = meterRegistry,
    )

    fun run() = consumer.run()

    companion object {
        /**
         * Trukket ut i companion object for å kunne testes uten å starte en KafkaConsumer.
         * @return om meldingen gjaldt tiltakspenger og ble lagret for første gang (brukes av testene).
         */
        fun consume(
            record: ConsumerRecord<out String?, out String?>,
            clock: Clock,
            lagreUtbetalingskvittering: (String?, String?, Kafkametadata, LocalDateTime) -> Boolean,
        ): Boolean {
            val kafkametadata = record.tilKafkametadata()
            if (kafkametadata.headere["fagsystem"] != "TILTAKSPENGER") return false
            return lagreUtbetalingskvittering(record.key(), record.value(), kafkametadata, nå(clock))
        }
    }
}
