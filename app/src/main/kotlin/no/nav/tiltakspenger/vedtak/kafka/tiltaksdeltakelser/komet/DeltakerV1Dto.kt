package no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.komet

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.Avbrutt
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.Deltar
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.Feilregistrert
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.Fullført
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.HarSluttet
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.IkkeAktuell
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.PåbegyntRegistrering
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.SøktInn
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.Venteliste
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.VenterPåOppstart
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.TiltakDeltakerstatus.Vurderes
import no.nav.tiltakspenger.vedtak.kafka.tiltaksdeltakelser.repository.TiltaksdeltakerKafkaDb
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
        val type: Type,
    ) {
        enum class Type {
            UTKAST_TIL_PAMELDING,
            AVBRUTT_UTKAST,
            VENTER_PA_OPPSTART,
            DELTAR,
            HAR_SLUTTET,
            IKKE_AKTUELL,
            FEILREGISTRERT,
            SOKT_INN,
            VURDERES,
            VENTELISTE,
            AVBRUTT,
            FULLFORT,
            PABEGYNT_REGISTRERING,
        }
    }

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

    private fun DeltakerStatusDto.Type.toTiltakDeltakerStatus(): TiltakDeltakerstatus {
        return when (this) {
            DeltakerStatusDto.Type.PABEGYNT_REGISTRERING,
            DeltakerStatusDto.Type.UTKAST_TIL_PAMELDING,
            -> PåbegyntRegistrering
            DeltakerStatusDto.Type.IKKE_AKTUELL,
            DeltakerStatusDto.Type.AVBRUTT_UTKAST,
            -> IkkeAktuell
            DeltakerStatusDto.Type.VENTER_PA_OPPSTART -> VenterPåOppstart
            DeltakerStatusDto.Type.DELTAR -> Deltar
            DeltakerStatusDto.Type.HAR_SLUTTET -> HarSluttet
            DeltakerStatusDto.Type.FEILREGISTRERT -> Feilregistrert
            DeltakerStatusDto.Type.SOKT_INN -> SøktInn
            DeltakerStatusDto.Type.VURDERES -> Vurderes
            DeltakerStatusDto.Type.VENTELISTE -> Venteliste
            DeltakerStatusDto.Type.AVBRUTT -> Avbrutt
            DeltakerStatusDto.Type.FULLFORT -> Fullført
        }
    }
}
