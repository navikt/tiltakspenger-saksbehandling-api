package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.komet

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.tiltak.KometDeltakerStatusType
import no.nav.tiltakspenger.libs.tiltak.toDeltakerStatusDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.toDomain
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse.TiltaksdeltakerHendelseId
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

    fun tilTiltaksdeltakerHendelse(sakId: SakId, tiltaksdeltakerId: TiltaksdeltakerId) =
        TiltaksdeltakerHendelse(
            id = TiltaksdeltakerHendelseId.random(),
            eksternDeltakerId = id.toString(),
            deltakelseFraOgMed = startDato,
            deltakelseTilOgMed = sluttDato,
            dagerPerUke = dagerPerUke,
            deltakelsesprosent = prosentStilling,
            deltakerstatus = status.type.toTiltakDeltakerStatus(),
            sakId = sakId,
            oppgaveId = null,
            oppgaveSistSjekket = null,
            internDeltakerId = tiltaksdeltakerId,
            behandlingId = null,
        )

    private fun KometDeltakerStatusType.toTiltakDeltakerStatus(): TiltakDeltakerstatus =
        this.toDeltakerStatusDTO().toDomain()
}
