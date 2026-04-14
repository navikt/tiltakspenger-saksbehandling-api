package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.arena

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaDeltakerStatusType
import no.nav.tiltakspenger.libs.arena.tiltak.toDTO
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.toDomain
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelseId
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class ArenaDeltakerMapper {
    private val log = KotlinLogging.logger { }

    fun mapArenaDeltaker(
        eksternId: String,
        arenaKafkaMessage: ArenaKafkaMessage,
        sakId: SakId,
        tiltaksdeltakerId: TiltaksdeltakerId,
        clock: Clock,
    ): TiltaksdeltakerHendelse? {
        if (arenaKafkaMessage.after != null) {
            return arenaKafkaMessage.after.tilTiltaksdeltakerHendelse(eksternId, sakId, tiltaksdeltakerId, clock)
        }

        if (arenaKafkaMessage.opType == OperationType.D) {
            log.warn { "Deltakelse med id $eksternId er slettet fra Arena" }
            return null
        } else {
            log.error { "Deltakelse med id $eksternId er ikke slettet, men mangler likevel deltakerinfo" }
            throw IllegalArgumentException()
        }
    }

    private fun ArenaDeltakerKafka.tilTiltaksdeltakerHendelse(
        eksternId: String,
        sakId: SakId,
        tiltaksdeltakerId: TiltaksdeltakerId,
        clock: Clock,
    ): TiltaksdeltakerHendelse {
        val deltakelseFraOgMed = DATO_FRA?.asValidatedLocalDate()
        return TiltaksdeltakerHendelse(
            id = TiltaksdeltakerHendelseId.random(),
            eksternDeltakerId = eksternId,
            deltakelseFraOgMed = deltakelseFraOgMed,
            deltakelseTilOgMed = DATO_TIL?.asValidatedLocalDate(),
            dagerPerUke = ANTALL_DAGER_PR_UKE,
            deltakelsesprosent = PROSENT_DELTID,
            deltakerstatus = DELTAKERSTATUSKODE.toTiltakDeltakerstatus(deltakelseFraOgMed, clock = clock),
            sakId = sakId,
            oppgaveId = null,
            oppgaveSistSjekket = null,
            internDeltakerId = tiltaksdeltakerId,
            behandlingId = null,
        )
    }

    private fun ArenaDeltakerStatusType.toTiltakDeltakerstatus(
        deltakelseFraOgMed: LocalDate?,
        clock: Clock,
    ): TiltakDeltakerstatus =
        this.toDTO(deltakelseFraOgMed, clock = clock).toDomain()

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
