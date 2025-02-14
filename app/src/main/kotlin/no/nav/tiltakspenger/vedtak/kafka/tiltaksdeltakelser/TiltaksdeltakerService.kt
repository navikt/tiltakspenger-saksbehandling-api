package no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser

import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.ports.SøknadRepo
import no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.arena.ArenaDeltakerMapper
import no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.repository.TiltaksdeltakerKafkaDb
import no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.repository.TiltaksdeltakerKafkaRepository

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
            val tiltaksdeltakerKafkaDb = arenaDeltakerMapper.mapArenaDeltaker(
                eksternId = eksternId,
                melding = melding,
                sakId = sakId,
            )
            if (tiltaksdeltakerKafkaDb != null) {
                lagreEllerOppdaterTiltaksdeltaker(tiltaksdeltakerKafkaDb)
                log.info { "Lagret melding for arenadeltaker med id $eksternId" }
            }
        } else {
            log.info { "Fant ingen sak knyttet til eksternId $eksternId, lagrer ikke" }
        }
    }

    private fun finnSakIdForTiltaksdeltaker(eksternId: String): SakId? {
        return søknadRepo.finnSakIdForTiltaksdeltakelse(eksternId)
    }

    private fun lagreEllerOppdaterTiltaksdeltaker(tiltaksdeltakerKafkaDb: TiltaksdeltakerKafkaDb) {
        tiltaksdeltakerKafkaRepository.lagre(tiltaksdeltakerKafkaDb)
    }
}
