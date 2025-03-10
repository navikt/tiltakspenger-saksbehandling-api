package no.nav.tiltakspenger.saksbehandling.kafka.tiltaksdeltakelser.arena

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.tiltakspenger.libs.arena.tiltak.ArenaDeltakerStatusType

data class ArenaKafkaMessage(
    @JsonProperty("op_type")
    val opType: OperationType,
    val after: ArenaDeltakerKafka?,
)

enum class OperationType {
    I,
    U,
    D,
}

data class ArenaDeltakerKafka(
    val DELTAKERSTATUSKODE: ArenaDeltakerStatusType,
    val DATO_FRA: String?,
    val DATO_TIL: String?,
    val PROSENT_DELTID: Float?,
    val ANTALL_DAGER_PR_UKE: Float?,
)
