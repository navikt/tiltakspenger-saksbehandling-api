package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.kafka.Consumer
import no.nav.tiltakspenger.libs.kafka.ManagedKafkaConsumer
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.config.KafkaConfigImpl
import no.nav.tiltakspenger.libs.kafka.config.LocalKafkaConfig
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.infra.setup.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.tilNyTilbakekrevingshendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo.TilbakekrevingHendelseRepo
import org.apache.kafka.common.serialization.StringDeserializer

private val logger = KotlinLogging.logger { }

class TilbakekrevingConsumer(
    private val tilbakekrevingHendelseRepo: TilbakekrevingHendelseRepo,
    private val sakRepo: SakRepo,
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

            val eksternFagsakId = hendelse.eksternFagsakId

            val sakId: SakId? = Either.catch {
                sakRepo.hentSakIdForSaksnummer(Saksnummer(eksternFagsakId))
            }.getOrElse {
                if (erFakeSak(eksternFagsakId)) {
                    logger.info { "Mottatt tilbakekrevingshendelse for fake sak $eksternFagsakId - ignorerer" }
                    return
                }

                logger.error { "Mottatt tilbakekrevingshendelse. Fant ikke sak for eksternFagsakId $eksternFagsakId, lagrer hendelse uten sakId" }
                null
            }

            val bleLagret = tilbakekrevingHendelseRepo.lagreNy(hendelse, sakId, key, value)

            if (!bleLagret) {
                logger.error { "Tilbakekrevingshendelse ble ikke lagret - ${hendelse.hendelsestype} / ${hendelse.eksternFagsakId} / ${hendelse.opprettet}" }
            } else {
                logger.info { "Lagret ny tilbakekrevingshendelse - type ${hendelse.hendelsestype}. sakId $sakId" }
            }
        }

        // Team tilbake sender noen ganger saker de har generert selv for å teste i dev
        private fun erFakeSak(eksternSakId: String): Boolean {
            return erDev && eksternSakId.startsWith(FAKE_SAK_PREFIX)
        }

        private val erDev: Boolean = Configuration.isDev()

        private const val FAKE_SAK_PREFIX = "BF"
    }
}
