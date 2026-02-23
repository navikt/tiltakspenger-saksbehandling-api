package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import no.nav.tiltakspenger.saksbehandling.klage.domene.Klageinstanshendelser
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.BehandlingFeilregistrert.KlagehendelseFeilregistrertType
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet.OmgjøringskravbehandlingAvsluttetUtfall
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlageinstanshendelseDTO.KlagehendelseFeilregistrertTypeDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlageinstanshendelseDTO.KlagehendelseKlagebehandlingAvsluttetUtfallDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlageinstanshendelseDTO.OmgjøringskravbehandlingAvsluttetUtfallDto
import java.time.LocalDateTime

sealed interface KlageinstanshendelseDTO {
    val klagehendelseId: String
    val klagebehandlingId: String
    val opprettet: LocalDateTime
    val sistEndret: LocalDateTime
    val eksternKlagehendelseId: String
    val avsluttetTidspunkt: LocalDateTime?
    val journalpostreferanser: List<String>

    data class KlagebehandlingAvsluttetDTO(
        override val klagehendelseId: String,
        override val klagebehandlingId: String,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
        override val eksternKlagehendelseId: String,
        override val avsluttetTidspunkt: LocalDateTime,
        override val journalpostreferanser: List<String>,
        val utfall: KlagehendelseKlagebehandlingAvsluttetUtfallDto,
    ) : KlageinstanshendelseDTO

    data class OmgjøringskravbehandlingAvsluttetDTO(
        override val klagehendelseId: String,
        override val klagebehandlingId: String,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
        override val eksternKlagehendelseId: String,
        override val avsluttetTidspunkt: LocalDateTime,
        override val journalpostreferanser: List<String>,
        val utfall: OmgjøringskravbehandlingAvsluttetUtfallDto,
    ) : KlageinstanshendelseDTO

    data class BehandlingFeilregistrertDTO(
        override val klagehendelseId: String,
        override val klagebehandlingId: String,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
        override val eksternKlagehendelseId: String,
        override val avsluttetTidspunkt: LocalDateTime? = null,
        override val journalpostreferanser: List<String> = emptyList(),
        val feilregistrertTidspunkt: LocalDateTime,
        val årsak: String,
        val navIdent: String,
        val type: KlagehendelseFeilregistrertTypeDto,
    ) : KlageinstanshendelseDTO

    enum class KlagehendelseKlagebehandlingAvsluttetUtfallDto {
        TRUKKET,
        RETUR,
        OPPHEVET,
        MEDHOLD,
        DELVIS_MEDHOLD,
        STADFESTELSE,
        UGUNST,
        AVVIST,
        HENLAGT,
    }

    enum class OmgjøringskravbehandlingAvsluttetUtfallDto {
        MEDHOLD_ETTER_FVL_35,
    }

    enum class KlagehendelseFeilregistrertTypeDto {
        KLAGE,
        ANKE,
        ANKE_I_TRYGDERETTEN,
        BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET,
        OMGJOERINGSKRAV,
    }
}

fun Klageinstanshendelser.toDTO(): List<KlageinstanshendelseDTO> {
    return this.map { it.toDTO() }
}

fun Klageinstanshendelse.toDTO(): KlageinstanshendelseDTO {
    return when (this) {
        is Klageinstanshendelse.KlagebehandlingAvsluttet -> KlageinstanshendelseDTO.KlagebehandlingAvsluttetDTO(
            klagehendelseId = klagehendelseId.toString(),
            klagebehandlingId = klagebehandlingId.toString(),
            opprettet = opprettet,
            sistEndret = sistEndret,
            eksternKlagehendelseId = eksternKlagehendelseId,
            avsluttetTidspunkt = avsluttetTidspunkt,
            journalpostreferanser = journalpostreferanser.map { it.toString() },
            utfall = utfall.toDTO(),
        )

        is OmgjøringskravbehandlingAvsluttet -> KlageinstanshendelseDTO.OmgjøringskravbehandlingAvsluttetDTO(
            klagehendelseId = klagehendelseId.toString(),
            klagebehandlingId = klagebehandlingId.toString(),
            opprettet = opprettet,
            sistEndret = sistEndret,
            eksternKlagehendelseId = eksternKlagehendelseId,
            avsluttetTidspunkt = avsluttetTidspunkt,
            journalpostreferanser = journalpostreferanser.map { it.toString() },
            utfall = utfall.toDTO(),
        )

        is Klageinstanshendelse.BehandlingFeilregistrert -> KlageinstanshendelseDTO.BehandlingFeilregistrertDTO(
            klagehendelseId = klagehendelseId.toString(),
            klagebehandlingId = klagebehandlingId.toString(),
            opprettet = opprettet,
            sistEndret = sistEndret,
            eksternKlagehendelseId = eksternKlagehendelseId,
            feilregistrertTidspunkt = feilregistrertTidspunkt,
            årsak = årsak,
            navIdent = navIdent,
            type = type.toDTO(),
        )
    }
}

private fun KlagehendelseKlagebehandlingAvsluttetUtfall.toDTO(): KlagehendelseKlagebehandlingAvsluttetUtfallDto {
    return when (this) {
        KlagehendelseKlagebehandlingAvsluttetUtfall.TRUKKET -> KlagehendelseKlagebehandlingAvsluttetUtfallDto.TRUKKET
        KlagehendelseKlagebehandlingAvsluttetUtfall.RETUR -> KlagehendelseKlagebehandlingAvsluttetUtfallDto.RETUR
        KlagehendelseKlagebehandlingAvsluttetUtfall.OPPHEVET -> KlagehendelseKlagebehandlingAvsluttetUtfallDto.OPPHEVET
        KlagehendelseKlagebehandlingAvsluttetUtfall.MEDHOLD -> KlagehendelseKlagebehandlingAvsluttetUtfallDto.MEDHOLD
        KlagehendelseKlagebehandlingAvsluttetUtfall.DELVIS_MEDHOLD -> KlagehendelseKlagebehandlingAvsluttetUtfallDto.DELVIS_MEDHOLD
        KlagehendelseKlagebehandlingAvsluttetUtfall.STADFESTELSE -> KlagehendelseKlagebehandlingAvsluttetUtfallDto.STADFESTELSE
        KlagehendelseKlagebehandlingAvsluttetUtfall.UGUNST -> KlagehendelseKlagebehandlingAvsluttetUtfallDto.UGUNST
        KlagehendelseKlagebehandlingAvsluttetUtfall.AVVIST -> KlagehendelseKlagebehandlingAvsluttetUtfallDto.AVVIST
        KlagehendelseKlagebehandlingAvsluttetUtfall.HENLAGT -> KlagehendelseKlagebehandlingAvsluttetUtfallDto.HENLAGT
    }
}

private fun OmgjøringskravbehandlingAvsluttetUtfall.toDTO(): OmgjøringskravbehandlingAvsluttetUtfallDto {
    return when (this) {
        OmgjøringskravbehandlingAvsluttetUtfall.MEDHOLD_ETTER_FVL_35 -> OmgjøringskravbehandlingAvsluttetUtfallDto.MEDHOLD_ETTER_FVL_35
    }
}

private fun KlagehendelseFeilregistrertType.toDTO(): KlagehendelseFeilregistrertTypeDto {
    return when (this) {
        KlagehendelseFeilregistrertType.KLAGE -> KlagehendelseFeilregistrertTypeDto.KLAGE
        KlagehendelseFeilregistrertType.ANKE -> KlagehendelseFeilregistrertTypeDto.ANKE
        KlagehendelseFeilregistrertType.ANKE_I_TRYGDERETTEN -> KlagehendelseFeilregistrertTypeDto.ANKE_I_TRYGDERETTEN
        KlagehendelseFeilregistrertType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET -> KlagehendelseFeilregistrertTypeDto.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET
        KlagehendelseFeilregistrertType.OMGJOERINGSKRAV -> KlagehendelseFeilregistrertTypeDto.OMGJOERINGSKRAV
    }
}
