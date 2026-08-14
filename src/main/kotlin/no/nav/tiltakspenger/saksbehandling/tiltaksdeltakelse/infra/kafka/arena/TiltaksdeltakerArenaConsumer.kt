package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.arena

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.kafka.infra.Consumer
import no.nav.tiltakspenger.libs.kafka.infra.KafkaConfig
import no.nav.tiltakspenger.libs.kafka.infra.ManagedKafkaConsumer
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.infra.setup.KAFKA_CONSUMER_GROUP_ID
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltaker
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseKilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerHendelsePostgresRepo
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Clock

private val logger = KotlinLogging.logger { }

class TiltaksdeltakerArenaConsumer(
    private val tiltaksdeltakerRepo: TiltaksdeltakerRepo,
    private val søknadRepo: SøknadRepo,
    private val tiltaksdeltakerHendelsePostgresRepo: TiltaksdeltakerHendelsePostgresRepo,
    private val clock: Clock,
    topic: String,
    groupId: String = KAFKA_CONSUMER_GROUP_ID,
    kafkaConfig: KafkaConfig,
    log: KLogger? = logger,
) : Consumer<String, String> {

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

    override suspend fun consume(key: String, value: String) {
        consume(
            deltakerId = key,
            melding = value,
            tiltaksdeltakerRepo = tiltaksdeltakerRepo,
            søknadRepo = søknadRepo,
            tiltaksdeltakerHendelsePostgresRepo = tiltaksdeltakerHendelsePostgresRepo,
            clock = clock,
        )
    }

    override fun run() = consumer.run()

    companion object {

        /**
         * @return id-en til hendelsen dersom den ble lagret, ellers null.
         */
        fun consume(
            deltakerId: String,
            melding: String,
            tiltaksdeltakerRepo: TiltaksdeltakerRepo,
            søknadRepo: SøknadRepo,
            tiltaksdeltakerHendelsePostgresRepo: TiltaksdeltakerHendelsePostgresRepo,
            clock: Clock,
        ): TiltaksdeltakerHendelseId? {
            logger.info { "Mottatt tiltaksdeltakelse fra arena med key $deltakerId" }
            val eksternId = "TA$deltakerId"

            val tiltaksdeltaker = tiltaksdeltakerRepo.hentTiltaksdeltaker(eksternId)
            if (tiltaksdeltaker == null) {
                logger.info { "Fant ingen tiltaksdeltaker knyttet til arena-deltaker med id $eksternId, lagrer ikke" }
                return null
            }

            val sakId = søknadRepo.finnSakIdForTiltaksdeltakelse(tiltaksdeltaker.id)
            if (sakId == null) {
                logger.error { "Fant ingen sak knyttet til arena-deltaker med id $eksternId, lagrer ikke" }
                return null
            }

            logger.info { "Fant sak $sakId for arena-deltaker med id $eksternId" }
            val arenaHendelseDTO = deserialize<ArenaHendelseDTO>(melding)
            val oppdatertEksternId = oppdaterEksternId(
                arenaEksternId = arenaHendelseDTO.after?.EKSTERN_ID,
                arenaId = eksternId,
                tiltaksdeltaker = tiltaksdeltaker,
                tiltaksdeltakerRepo = tiltaksdeltakerRepo,
            )

            val tiltaksdeltakerHendelse = arenaHendelseDTO.tilTiltaksdeltakerHendelse(
                eksternId = oppdatertEksternId,
                sakId = sakId,
                tiltaksdeltakerId = tiltaksdeltaker.id,
                clock = clock,
            ) ?: return null

            tiltaksdeltakerHendelsePostgresRepo.lagre(
                tiltaksdeltakerHendelse,
                melding,
                TiltaksdeltakerHendelseKilde.Arena,
            )
            logger.info { "Lagret melding for arenadeltaker med id $oppdatertEksternId" }
            return tiltaksdeltakerHendelse.id
        }

        private fun oppdaterEksternId(
            arenaEksternId: String?,
            arenaId: String,
            tiltaksdeltaker: Tiltaksdeltaker,
            tiltaksdeltakerRepo: TiltaksdeltakerRepo,
        ): String {
            if (arenaEksternId.isNullOrEmpty()) {
                return arenaId
            }

            logger.info { "Tiltaksdeltakelse med eksternId $arenaId og internId ${tiltaksdeltaker.id} er flyttet ut av Arena med id $arenaEksternId" }
            tiltaksdeltakerRepo.oppdaterEksternIdForTiltaksdeltaker(
                tiltaksdeltaker = tiltaksdeltaker.copy(
                    eksternId = arenaEksternId,
                    utdatertEksternId = arenaId,
                ),
            )
            logger.info { "Har oppdatert eksternId for tiltaksdeltakelse med internId ${tiltaksdeltaker.id} og ny eksternId $arenaEksternId" }
            return arenaEksternId
        }
    }
}
