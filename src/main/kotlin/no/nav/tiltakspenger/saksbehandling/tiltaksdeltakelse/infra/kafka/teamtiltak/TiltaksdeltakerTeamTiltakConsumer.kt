package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.teamtiltak

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.kafka.infra.Consumer
import no.nav.tiltakspenger.libs.kafka.infra.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.infra.ManagedKafkaConsumer
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.infra.setup.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseKilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerHendelsePostgresRepo
import org.apache.kafka.common.serialization.StringDeserializer

private val logger = KotlinLogging.logger { }

class TiltaksdeltakerTeamTiltakConsumer(
    private val tiltaksdeltakerRepo: TiltaksdeltakerRepo,
    private val søknadRepo: SøknadRepo,
    private val tiltaksdeltakerHendelsePostgresRepo: TiltaksdeltakerHendelsePostgresRepo,
    topic: String,
    groupId: String = KAFKA_CONSUMER_GROUP_ID,
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
    )

    override suspend fun consume(key: String, value: String?) {
        consume(
            deltakerId = key,
            melding = value,
            tiltaksdeltakerRepo = tiltaksdeltakerRepo,
            søknadRepo = søknadRepo,
            tiltaksdeltakerHendelsePostgresRepo = tiltaksdeltakerHendelsePostgresRepo,
        )
    }

    override fun run() = consumer.run()

    companion object {

        /**
         * @return id-en til hendelsen dersom den ble lagret, ellers null.
         */
        fun consume(
            deltakerId: String,
            melding: String?,
            tiltaksdeltakerRepo: TiltaksdeltakerRepo,
            søknadRepo: SøknadRepo,
            tiltaksdeltakerHendelsePostgresRepo: TiltaksdeltakerHendelsePostgresRepo,
        ): TiltaksdeltakerHendelseId? {
            logger.info { "Mottatt tiltaksdeltakelse fra team tiltak med key $deltakerId" }
            if (melding == null) {
                logger.warn { "Ignorerer tombstonet deltaker med id $deltakerId" }
                return null
            }

            val tiltaksdeltakerId = tiltaksdeltakerRepo.hentInternId(deltakerId)
            if (tiltaksdeltakerId == null) {
                logger.info { "Fant ingen tiltaksdeltaker knyttet til team tiltak-deltaker med id $deltakerId, lagrer ikke" }
                return null
            }

            val sakId = søknadRepo.finnSakIdForTiltaksdeltakelse(tiltaksdeltakerId)
            if (sakId == null) {
                logger.error { "Fant ingen sak knyttet til team tiltak-deltaker med id $deltakerId, lagrer ikke" }
                return null
            }

            logger.info { "Fant sak $sakId for team tiltak-deltaker med id $deltakerId" }
            val hendelseDTO = deserialize<TeamTiltakHendelseDTO>(melding)
            val tiltaksdeltakerHendelse = hendelseDTO.tilTiltaksdeltakerHendelse(sakId, tiltaksdeltakerId)
            tiltaksdeltakerHendelsePostgresRepo.lagre(
                tiltaksdeltakerHendelse,
                melding,
                TiltaksdeltakerHendelseKilde.TeamTiltak,
            )
            logger.info { "Lagret melding for team tiltak-deltaker med id $deltakerId" }
            return tiltaksdeltakerHendelse.id
        }
    }
}
