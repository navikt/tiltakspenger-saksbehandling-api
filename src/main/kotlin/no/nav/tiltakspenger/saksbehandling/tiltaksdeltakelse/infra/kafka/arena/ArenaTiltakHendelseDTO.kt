package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.arena

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaDeltakerStatusType
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId

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
    ): TiltaksdeltakerHendelse? {
        if (after != null) {
            return TiltaksdeltakerHendelse(
                id = TiltaksdeltakerHendelseId.random(),
                eksternDeltakerId = eksternId,
                sakId = sakId,
                internDeltakerId = tiltaksdeltakerId,
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
