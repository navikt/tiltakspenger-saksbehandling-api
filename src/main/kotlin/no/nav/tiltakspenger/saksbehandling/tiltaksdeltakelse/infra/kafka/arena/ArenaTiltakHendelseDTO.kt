package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.arena

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaDeltakerStatusType
import no.nav.tiltakspenger.libs.arena.tiltak.toDTO
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.toDomain
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class ArenaHendelseDTO(
    @param:JsonProperty("op_type")
    val opType: ArenaOperationType,
    val after: ArenaDeltakerDTO?,
) {
    private val log = KotlinLogging.logger { }

    fun tilTiltaksdeltakerHendelse(
        eksternId: String,
        sakId: SakId,
        tiltaksdeltakerId: TiltaksdeltakerId,
        clock: Clock,
    ): TiltaksdeltakerHendelse? {
        if (after != null) {
            val deltakelseFraOgMed = after.DATO_FRA?.asValidatedLocalDate()
            return TiltaksdeltakerHendelse(
                id = TiltaksdeltakerHendelseId.random(),
                eksternDeltakerId = eksternId,
                deltakelseFraOgMed = deltakelseFraOgMed,
                deltakelseTilOgMed = after.DATO_TIL?.asValidatedLocalDate(),
                dagerPerUke = after.ANTALL_DAGER_PR_UKE,
                deltakelsesprosent = after.PROSENT_DELTID,
                deltakerstatus = after.DELTAKERSTATUSKODE.toTiltakDeltakerstatus(deltakelseFraOgMed, clock = clock),
                sakId = sakId,
                oppgaveId = null,
                internDeltakerId = tiltaksdeltakerId,
                behandlingId = null,
            )
        }

        if (opType == ArenaOperationType.D) {
            log.warn { "Deltakelse med id $eksternId er slettet fra Arena" }
            return null
        } else {
            log.error { "Deltakelse med id $eksternId er ikke slettet, men mangler likevel deltakerinfo" }
            throw IllegalArgumentException()
        }
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

enum class ArenaOperationType {
    I,
    U,
    D,
}

data class ArenaDeltakerDTO(
    val DELTAKERSTATUSKODE: ArenaDeltakerStatusType,
    val DATO_FRA: String?,
    val DATO_TIL: String?,
    val PROSENT_DELTID: Float?,
    val ANTALL_DAGER_PR_UKE: Float?,
    val EKSTERN_ID: String?,
)
