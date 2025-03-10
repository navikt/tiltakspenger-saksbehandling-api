package no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser

import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.arena.ArenaDeltakerMapper
import no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.arena.ArenaKafkaMessage
import no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.komet.DeltakerV1Dto
import no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.repository.TiltaksdeltakerKafkaDb
import no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.repository.TiltaksdeltakerKafkaRepository
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.SøknadRepo
import java.util.UUID

class TiltaksdeltakerService(
    private val tiltaksdeltakerKafkaRepository: TiltaksdeltakerKafkaRepository,
    private val søknadRepo: SøknadRepo,
    private val arenaDeltakerMapper: ArenaDeltakerMapper,
) {
    private val log = KotlinLogging.logger { }

    fun behandleMottattArenadeltaker(deltakerId: String, melding: String) {
        val eksternId = "TA$deltakerId"
        val sakId = finnSakIdForTiltaksdeltaker(eksternId)
        if (sakId != null) {
            log.info { "Fant sakId $sakId for arena-deltaker med id $eksternId" }
            val arenaKafkaMessage = objectMapper.readValue<ArenaKafkaMessage>(melding)
            val tiltaksdeltakerKafkaDb = arenaDeltakerMapper.mapArenaDeltaker(
                eksternId = eksternId,
                arenaKafkaMessage = arenaKafkaMessage,
                sakId = sakId,
            )
            if (tiltaksdeltakerKafkaDb != null) {
                lagreEllerOppdaterTiltaksdeltaker(tiltaksdeltakerKafkaDb, objectMapper.writeValueAsString(arenaKafkaMessage))
                log.info { "Lagret melding for arenadeltaker med id $eksternId" }
            }
        } else {
            log.info { "Fant ingen sak knyttet til eksternId $eksternId, lagrer ikke" }
        }
    }

    fun behandleMottattKometdeltaker(deltakerId: UUID, melding: String) {
        val sakId = finnSakIdForTiltaksdeltaker(deltakerId.toString())
        if (sakId != null) {
            log.info { "Fant sakId $sakId for komet-deltaker med id $deltakerId" }
            val deltakerV1Dto = objectMapper.readValue<DeltakerV1Dto>(melding)
            val tiltaksdeltakerKafkaDb = deltakerV1Dto.toTiltaksdeltakerKafkaDb(sakId)
            lagreEllerOppdaterTiltaksdeltaker(tiltaksdeltakerKafkaDb, objectMapper.writeValueAsString(deltakerV1Dto))
            log.info { "Lagret melding for kometdeltaker med id $deltakerId" }
        } else {
            log.info { "Fant ingen sak knyttet til eksternId $deltakerId, lagrer ikke" }
        }
    }

    private fun finnSakIdForTiltaksdeltaker(eksternId: String): SakId? {
        return søknadRepo.finnSakIdForTiltaksdeltakelse(eksternId)
    }

    private fun lagreEllerOppdaterTiltaksdeltaker(tiltaksdeltakerKafkaDb: TiltaksdeltakerKafkaDb, melding: String) {
        tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb, melding)
    }
}
