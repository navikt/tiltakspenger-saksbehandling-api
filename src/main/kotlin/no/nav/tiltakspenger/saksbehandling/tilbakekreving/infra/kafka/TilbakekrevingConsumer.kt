package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.kafka.Consumer
import no.nav.tiltakspenger.libs.kafka.ManagedKafkaConsumer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.kafka.config.LocalKafkaConfig
import no.nav.tiltakspenger.libs.texas.log
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.tilNyTilbakekrevingshendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingHendelseRepo
import org.apache.kafka.common.serialization.StringDeserializer

private val logger = KotlinLogging.logger { }

class TilbakekrevingConsumer(
    private val tilbakekrevingHendelseRepo: TilbakekrevingHendelseRepo,
    private val sakRepo: SakRepo,
    topic: String,
    groupId: String = "$KAFKA_CONSUMER_GROUP_ID-v3",
    kafkaConfig: KafkaConfig = if (Configuration.isNais()) KafkaConfigImpl(autoOffsetReset = "earliest") else LocalKafkaConfig(),
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
        consume(key, value, tilbakekrevingHendelseRepo, sakRepo)
    }

    override fun run() = consumer.run()

    companion object {
        fun consume(
            key: String,
            value: String?,
            tilbakekrevingHendelseRepo: TilbakekrevingHendelseRepo,
            sakRepo: SakRepo,
        ) {
            // OBS: Merk at key er fødselsnummer, så det skal ikke logges.
            if (value == null) {
                logger.warn { "Mottatt tilbakekrevingshendelse uten value, hendelsen forkastes." }
                return
            }

            val hendelse = value.tilNyTilbakekrevingshendelse(key).getOrElse {
                logger.error(it) { "Mottatt tilbakekrevingshendelse hvor vi ikke klarte deserialisere. Denne vil prøves på nytt." }
                throw it
            }

            if (hendelse == null) {
                logger.debug { "Mottatt tilbakekrevingshendelse som vi tp-sak har produsert, hendelsen forkastes." }
                return
            }

            val sakId: SakId? = Either.catch {
                sakRepo.hentSakIdForSaksnummer(Saksnummer(hendelse.eksternFagsakId))
            }.getOrElse {
                logger.error { "Mottatt tilbakekrevingshendelse. Fant ikke sak for eksternFagsakId ${hendelse.eksternFagsakId}, lagrer hendelse uten sakId" }
                null
            }

            val bleLagret = tilbakekrevingHendelseRepo.lagreNy(hendelse, sakId, key, value)

            if (!bleLagret) {
                logger.debug { "Tilbakekrevingshendelse type ${hendelse.hendelsestype}. sakId $sakId" }
            } else {
                logger.info { "Lagret ny tilbakekrevingshendelse type ${hendelse.hendelsestype}. sakId $sakId" }
            }
        }
    }
}
