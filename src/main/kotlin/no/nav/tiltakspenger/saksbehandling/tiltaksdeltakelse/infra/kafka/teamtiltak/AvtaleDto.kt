package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.teamtiltak

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO.DeltakerStatusDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.toDomain
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.repository.TiltaksdeltakerKafkaDb
import java.time.LocalDate
import java.util.UUID

data class AvtaleDto(
    val avtaleId: UUID,
    val hendelseType: HendelseType,
    val avtaleStatus: AvtaleStatus,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val stillingprosent: Double?,
    val antallDagerPerUke: Double?,
    val feilregistrert: Boolean,
) {
    enum class AvtaleStatus {
        PÅBEGYNT,
        MANGLER_GODKJENNING,
        KLAR_FOR_OPPSTART,
        GJENNOMFØRES,
        AVSLUTTET,
        AVBRUTT,
        ANNULLERT,
    }

    enum class HendelseType {
        OPPRETTET,
        OPPRETTET_AV_ARENA,
        GODKJENT_AV_ARBEIDSGIVER,
        GODKJENT_AV_VEILEDER,
        GODKJENT_AV_DELTAKER,
        SIGNERT_AV_MENTOR,
        GODKJENT_PAA_VEGNE_AV,
        GODKJENT_PAA_VEGNE_AV_DELTAKER_OG_ARBEIDSGIVER,
        GODKJENT_PAA_VEGNE_AV_ARBEIDSGIVER,
        GODKJENNINGER_OPPHEVET_AV_ARBEIDSGIVER,
        GODKJENNINGER_OPPHEVET_AV_VEILEDER,
        DELT_MED_DELTAKER,
        DELT_MED_ARBEIDSGIVER,
        DELT_MED_MENTOR,
        ENDRET,
        ENDRET_AV_ARENA,
        AVBRUTT,
        ANNULLERT,
        LÅST_OPP,
        GJENOPPRETTET,
        OPPRETTET_AV_ARBEIDSGIVER,
        NY_VEILEDER,
        AVTALE_FORDELT,
        TILSKUDDSPERIODE_AVSLATT,
        TILSKUDDSPERIODE_GODKJENT,
        AVTALE_FORKORTET,
        AVTALE_FORKORTET_AV_ARENA,
        AVTALE_FORLENGET,
        AVTALE_FORLENGET_AV_ARENA,
        MÅL_ENDRET,
        INKLUDERINGSTILSKUDD_ENDRET,
        OM_MENTOR_ENDRET,
        TILSKUDDSBEREGNING_ENDRET,
        KONTAKTINFORMASJON_ENDRET,
        STILLINGSBESKRIVELSE_ENDRET,
        KID_OG_KONTONUMMER_ENDRET,
        OPPFØLGING_OG_TILRETTELEGGING_ENDRET,
        AVTALE_INNGÅTT,
        REFUSJON_KLAR,
        REFUSJON_KLAR_REVARSEL,
        REFUSJON_FRIST_FORLENGET,
        REFUSJON_KORRIGERT,
        VARSLER_SETT,
        AVTALE_SLETTET,
        GODKJENT_FOR_ETTERREGISTRERING,
        FJERNET_ETTERREGISTRERING,
        STATUSENDRING,
        DELTAKERS_GODKJENNING_OPPHEVET_AV_VEILEDER,
        DELTAKERS_GODKJENNING_OPPHEVET_AV_ARBEIDSGIVER,
        ARBEIDSGIVERS_GODKJENNING_OPPHEVET_AV_VEILEDER,
        UTLOPER_OM_1_UKE,
        UTLOPER_OM_24_TIMER,
        OPPFØLGING_AV_TILTAK_KREVES,
        OPPFØLGING_AV_TILTAK_UTFØRT,
        PATCH,
        OPPDATERTE_AVTALEKRAV,
    }

    fun toTiltaksdeltakerKafkaDb(sakId: SakId, tiltaksdeltakerId: TiltaksdeltakerId) =
        TiltaksdeltakerKafkaDb(
            id = avtaleId.toString(),
            deltakelseFraOgMed = startDato,
            deltakelseTilOgMed = sluttDato,
            dagerPerUke = antallDagerPerUke?.toFloat(),
            deltakelsesprosent = stillingprosent?.toFloat(),
            deltakerstatus = this.toTiltakDeltakerStatus(),
            sakId = sakId,
            oppgaveId = null,
            oppgaveSistSjekket = null,
            tiltaksdeltakerId = tiltaksdeltakerId,
        )
}

fun AvtaleDto.toTiltakDeltakerStatus(): TiltakDeltakerstatus =
    this.avtaleStatus.toDeltakerStatusDTO(feilregistrert).toDomain()

fun AvtaleDto.AvtaleStatus.toDeltakerStatusDTO(feilregistrert: Boolean): DeltakerStatusDTO = when (this) {
    AvtaleDto.AvtaleStatus.PÅBEGYNT -> DeltakerStatusDTO.PABEGYNT_REGISTRERING

    AvtaleDto.AvtaleStatus.MANGLER_GODKJENNING -> DeltakerStatusDTO.SOKT_INN

    AvtaleDto.AvtaleStatus.KLAR_FOR_OPPSTART -> DeltakerStatusDTO.VENTER_PA_OPPSTART

    AvtaleDto.AvtaleStatus.GJENNOMFØRES -> DeltakerStatusDTO.DELTAR

    AvtaleDto.AvtaleStatus.AVSLUTTET -> DeltakerStatusDTO.HAR_SLUTTET

    AvtaleDto.AvtaleStatus.AVBRUTT -> DeltakerStatusDTO.AVBRUTT

    AvtaleDto.AvtaleStatus.ANNULLERT -> if (feilregistrert) {
        DeltakerStatusDTO.FEILREGISTRERT
    } else {
        DeltakerStatusDTO.IKKE_AKTUELL
    }
}
