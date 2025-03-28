package no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.komet

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.tiltak.KometDeltakerStatusType
import no.nav.tiltakspenger.libs.tiltak.toDeltakerStatusDTO
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.tiltak.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.http.toDomain
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.kafka.repository.TiltaksdeltakerKafkaDb
import java.time.LocalDate
import java.util.UUID

data class DeltakerV1Dto(
    val id: UUID,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val status: DeltakerStatusDto,
    val dagerPerUke: Float?,
    val prosentStilling: Float?,
) {
    data class DeltakerStatusDto(
        val type: KometDeltakerStatusType,
    )

    fun toTiltaksdeltakerKafkaDb(sakId: SakId) =
        TiltaksdeltakerKafkaDb(
            id = id.toString(),
            deltakelseFraOgMed = startDato,
            deltakelseTilOgMed = sluttDato,
            dagerPerUke = dagerPerUke,
            deltakelsesprosent = prosentStilling,
            deltakerstatus = status.type.toTiltakDeltakerStatus(),
            sakId = sakId,
            oppgaveId = null,
            oppgaveSistSjekket = null,
        )

    private fun KometDeltakerStatusType.toTiltakDeltakerStatus(): TiltakDeltakerstatus =
        this.toDeltakerStatusDTO().toDomain()
}
