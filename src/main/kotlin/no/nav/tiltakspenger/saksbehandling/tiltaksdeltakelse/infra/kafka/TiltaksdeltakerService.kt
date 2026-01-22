package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.arena.ArenaDeltakerMapper
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.arena.ArenaKafkaMessage
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.komet.DeltakerV1Dto
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository.TiltaksdeltakerKafkaDb
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository.TiltaksdeltakerKafkaRepository
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerRepo
import java.util.UUID

class TiltaksdeltakerService(
    private val tiltaksdeltakerKafkaRepository: TiltaksdeltakerKafkaRepository,
    private val søknadRepo: SøknadRepo,
    private val arenaDeltakerMapper: ArenaDeltakerMapper,
    private val tiltaksdeltakerRepo: TiltaksdeltakerRepo,
) {
    private val log = KotlinLogging.logger { }

    fun behandleMottattArenadeltaker(deltakerId: String, melding: String) {
        val eksternId = "TA$deltakerId"
        val tiltaksdeltaker = tiltaksdeltakerRepo.hentTiltaksdeltaker(eksternId)
        val sakId = tiltaksdeltaker?.let { finnSakIdForTiltaksdeltaker(it.id) }
        if (tiltaksdeltaker != null && sakId != null) {
            log.info { "Fant sakId $sakId for arena-deltaker med id $eksternId" }
            val arenaKafkaMessage = objectMapper.readValue<ArenaKafkaMessage>(melding)

            val nyEksternId = arenaKafkaMessage.after?.EKSTERN_ID
            if (nyEksternId != null && nyEksternId.isNotEmpty() && tiltaksdeltaker.tiltakstype != TiltakResponsDTO.TiltakType.ARBTREN) {
                log.info { "Tiltaksdeltakelse med eksternId $eksternId og internId ${tiltaksdeltaker.id} er flyttet ut av Arena med id $nyEksternId" }
                tiltaksdeltakerRepo.oppdaterEksternIdForTiltaksdeltaker(
                    tiltaksdeltaker = tiltaksdeltaker.copy(
                        eksternId = nyEksternId,
                        utdatertEksternId = eksternId,
                    ),
                )
                log.info { "Har oppdatert eksternId for tiltaksdeltakelse med internId ${tiltaksdeltaker.id} og ny eksternId $nyEksternId" }
            }
            val tiltaksdeltakerKafkaDb = arenaDeltakerMapper.mapArenaDeltaker(
                eksternId = nyEksternId ?: eksternId,
                arenaKafkaMessage = arenaKafkaMessage,
                sakId = sakId,
                tiltaksdeltakerId = tiltaksdeltaker.id,
            )
            if (tiltaksdeltakerKafkaDb != null) {
                lagreEllerOppdaterTiltaksdeltaker(tiltaksdeltakerKafkaDb, objectMapper.writeValueAsString(arenaKafkaMessage))
                log.info { "Lagret melding for arenadeltaker med id ${nyEksternId ?: eksternId}" }
            }
        } else {
            log.info { "Fant ingen sak eller intern deltakerid knyttet til eksternId $eksternId, lagrer ikke" }
        }
    }

    fun behandleMottattKometdeltaker(deltakerId: UUID, melding: String) {
        val tiltaksdeltakerId = tiltaksdeltakerRepo.hentInternId(deltakerId.toString())
        val sakId = tiltaksdeltakerId?.let { finnSakIdForTiltaksdeltaker(it) }
        if (tiltaksdeltakerId != null && sakId != null) {
            log.info { "Fant sakId $sakId for komet-deltaker med id $deltakerId" }
            val deltakerV1Dto = objectMapper.readValue<DeltakerV1Dto>(melding)
            val tiltaksdeltakerKafkaDb = deltakerV1Dto.toTiltaksdeltakerKafkaDb(sakId, tiltaksdeltakerId)
            lagreEllerOppdaterTiltaksdeltaker(tiltaksdeltakerKafkaDb, objectMapper.writeValueAsString(deltakerV1Dto))
            log.info { "Lagret melding for kometdeltaker med id $deltakerId" }
        } else {
            log.info { "Fant ingen sak eller intern deltakerid knyttet til eksternId $deltakerId, lagrer ikke" }
        }
    }

    private fun finnSakIdForTiltaksdeltaker(tiltaksdeltakerId: TiltaksdeltakerId): SakId? {
        return søknadRepo.finnSakIdForTiltaksdeltakelse(tiltaksdeltakerId)
    }

    private fun lagreEllerOppdaterTiltaksdeltaker(tiltaksdeltakerKafkaDb: TiltaksdeltakerKafkaDb, melding: String) {
        tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb, melding)
    }
}
