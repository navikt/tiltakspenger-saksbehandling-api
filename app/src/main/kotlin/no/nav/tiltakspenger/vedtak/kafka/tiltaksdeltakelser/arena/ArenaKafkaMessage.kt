package no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.arena

import com.fasterxml.jackson.annotation.JsonProperty

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
    val DELTAKERSTATUSKODE: DeltakerStatusKodeArena,
    val DATO_FRA: String?,
    val DATO_TIL: String?,
    val PROSENT_DELTID: Float?,
    val ANTALL_DAGER_PR_UKE: Float?,
)

enum class DeltakerStatusKodeArena {
    DELAVB,
    FULLF,
    GJENN_AVB,
    GJENN_AVL,
    IKKEM,
    IKKAKTUELL,
    AVSLAG,
    NEITAKK,
    GJENN,
    TILBUD,
    VENTELISTE,
    AKTUELL,
    JATAKK,
    INFOMOETE,
    FEILREG,
}
