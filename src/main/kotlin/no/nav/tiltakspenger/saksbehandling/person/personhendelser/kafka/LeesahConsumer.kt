package no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.tiltakspenger.libs.kafka.Consumer
import no.nav.tiltakspenger.libs.kafka.ManagedKafkaConsumer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.kafka.config.LocalKafkaConfig
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.saksbehandling.person.personhendelser.PersonhendelseService
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

/**
 * Konsument for pdl.leesah-v1 (PDL personhendelser for hele Norges befolkning).
 * For identhendelser (som er en egen topic), se: [no.nav.tiltakspenger.saksbehandling.person.identhendelser.kafka.AktorV2Consumer].
 * Docs: https://pdl-docs.ansatt.nav.no/ekstern/index.html#livshendelser_pa_kafka
 * Skjema master: https://github.com/navikt/pdl/blob/master/libs/contract-pdl-avro/src/main/avro/no/nav/person/pdl/leesah/Personhendelse.avdl
 * Skjema kopi internt: src/main/avro/Personhendelse.avdl
 *
 * Høyt volum av hendelser.
 * Det produseres tidvis store batcher (millionstørrelse), spesielt i dev.
 * Vi er kun interessert i et lite subset (DOEDSFALL_V1 og ADRESSEBESKYTTELSE_V1) for fnr som har en sak hos oss.
 * Optimaliseringer:
 *   - Et billig fnr → sakId-oppslag i [PersonhendelseService].
 *   - Større poll-batches enn lib-default (MAX_POLL_RECORDS=1) for å redusere round-trip-overhead mot broker / commit.
 *     Vi overstyrer derfor `max.poll.records` lokalt.
 *   - Parallellitet via flere replicas (samme `group.id`) — se `.nais/vars/dev.yml` og `prod.yml`.
 *     PDL eier topic-partisjoneringen, så maks parallellitet = antall partisjoner.
 */
class LeesahConsumer(
    private val personhendelseService: PersonhendelseService,
    topic: String,
    groupId: String = KAFKA_CONSUMER_GROUP_ID,
    kafkaConfig: KafkaConfig = if (Configuration.isNais()) KafkaConfigImpl(autoOffsetReset = "none") else LocalKafkaConfig(),
    log: KLogger? = KotlinLogging.logger {},
) : Consumer<String, Personhendelse> {

    private val consumer = ManagedKafkaConsumer(
        kanLoggeKey = false,
        topic = topic,
        config = kafkaConfig.avroConsumerConfig(
            keyDeserializer = StringDeserializer(),
            valueDeserializer = KafkaAvroDeserializer(),
            groupId = groupId,
            useSpecificAvroReader = true,
        ) + mapOf(
            // Override lib-default på 1.
            // Lib-default er konservativ for at en feilende record skal gi minimal re-prosessering, men ManagedKafkaConsumer commit-er uansett ikke før hele batchen er ok, så vi mister ingen at-least-once-garantier ved å øke.
            // 100 reduserer poll/commit-overhead vesentlig på et høyvolums-topic som pdl.leesah-v1.
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 100,
        ),
        log = log,
        consume = ::consume,
    )

    // TODO jah: En optimaliseringsmulighet her er å få en liste med key/value-par og gjøre en batch-query mot databasen, for deretter og behandle de vi får treff på 1 og 1.
    override suspend fun consume(key: String, value: Personhendelse) {
        // All filtrering på opplysningstype / innhold ligger i PersonhendelseService, slik at den kan dekkes av enhetstester.
        personhendelseService.behandlePersonhendelse(value)
    }

    override fun run() = consumer.run()
}
