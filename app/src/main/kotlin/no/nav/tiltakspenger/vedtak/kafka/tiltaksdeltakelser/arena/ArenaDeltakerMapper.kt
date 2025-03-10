package no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.arena

import mu.KotlinLogging
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaDeltakerStatusType
import no.nav.tiltakspenger.libs.arena.tiltak.toDTO
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.vedtak.clients.tiltak.toDomain
import no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.repository.TiltaksdeltakerKafkaDb
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.tiltak.TiltakDeltakerstatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class ArenaDeltakerMapper {
    private val log = KotlinLogging.logger { }

    fun mapArenaDeltaker(
        eksternId: String,
        arenaKafkaMessage: ArenaKafkaMessage,
        sakId: SakId,
    ): TiltaksdeltakerKafkaDb? {
        arenaKafkaMessage.after?.let { return it.toTiltaksdeltakerKafkaDb(eksternId, sakId) }

        if (arenaKafkaMessage.opType == OperationType.D) {
            log.warn { "Deltakelse med id $eksternId er slettet fra Arena" }
            return null
        } else {
            log.error { "Deltakelse med id $eksternId er ikke slettet, men mangler likevel deltakerinfo" }
            throw IllegalArgumentException()
        }
    }

    private fun ArenaDeltakerKafka.toTiltaksdeltakerKafkaDb(eksternId: String, sakId: SakId): TiltaksdeltakerKafkaDb {
        val deltakelseFraOgMed = DATO_FRA?.asValidatedLocalDate()
        return TiltaksdeltakerKafkaDb(
            id = eksternId,
            deltakelseFraOgMed = deltakelseFraOgMed,
            deltakelseTilOgMed = DATO_TIL?.asValidatedLocalDate(),
            dagerPerUke = ANTALL_DAGER_PR_UKE,
            deltakelsesprosent = PROSENT_DELTID,
            deltakerstatus = DELTAKERSTATUSKODE.toTiltakDeltakerstatus(deltakelseFraOgMed),
            sakId = sakId,
            oppgaveId = null,
            oppgaveSistSjekket = null,
        )
    }

    private fun ArenaDeltakerStatusType.toTiltakDeltakerstatus(deltakelseFraOgMed: LocalDate?): TiltakDeltakerstatus =
        this.toDTO(deltakelseFraOgMed).toDomain()

    private fun String.asValidatedLocalDate(): LocalDate {
        try {
            return this.asLocalDate()
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("$this kan ikke parses til LocalDate")
        }
    }

    private fun String.asLocalDate(): LocalDate {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return LocalDate.parse(this, formatter)
    }
}
