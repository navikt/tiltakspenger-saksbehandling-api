package no.nav.tiltakspenger.saksbehandling.klage.infra.kafka

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.KlagehendelseId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.BehandlingFeilregistrert.KlagehendelseFeilregistrertType
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall
import java.time.LocalDateTime

private data class KlageinstanshendelseDto(
    val eventId: String,
    val kildeReferanse: String,
    val kilde: String,
    val kabalReferanse: String,
    val type: KlageinstanshendelseType,
    val detaljer: DetaljerDto,
) {
    enum class KlageinstanshendelseType {
        KLAGEBEHANDLING_AVSLUTTET,
        OMGJOERINGSKRAVBEHANDLING_AVSLUTTET,
        BEHANDLING_FEILREGISTRERT,
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class DetaljerDto(
        val klagebehandlingAvsluttet: KlagebehandlingAvsluttetDto? = null,
        val omgjoeringskravbehandlingAvsluttet: OmgjoeringskravbehandlingAvsluttetDto? = null,
        val behandlingFeilregistrertDetaljer: BehandlingFeilregistrertDto? = null,
    )

    data class KlagebehandlingAvsluttetDto(
        val avsluttet: String,
        val utfall: Utfall,
        val journalpostReferanser: List<String>,
    )

    data class OmgjoeringskravbehandlingAvsluttetDto(
        val avsluttet: String,
        val utfall: String,
        val journalpostReferanser: List<String>,
    )

    data class BehandlingFeilregistrertDto(
        val feilregistrert: String,
        val navIdent: String,
        val reason: String,
        val type: FeilregistrertType,
    )

    enum class Utfall {
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

    enum class FeilregistrertType {
        KLAGE,
        ANKE,
        ANKE_I_TRYGDERETTEN,
        BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET,
        OMGJOERINGSKRAV,
    }
}

fun String.toKlageinstanshendelse(
    klagehendelseId: KlagehendelseId,
    opprettet: LocalDateTime,
    sistEndret: LocalDateTime,
): Klageinstanshendelse {
    val dto = deserialize<KlageinstanshendelseDto>(this)
    val klagebehandlingId = KlagebehandlingId.fromString(dto.kildeReferanse)
    return when (dto.type) {
        KlageinstanshendelseDto.KlageinstanshendelseType.KLAGEBEHANDLING_AVSLUTTET -> {
            val detaljer = requireNotNull(dto.detaljer.klagebehandlingAvsluttet) {
                "Mangler klagebehandlingAvsluttet-detaljer for eventId=${dto.eventId}"
            }
            Klageinstanshendelse.KlagebehandlingAvsluttet(
                klagehendelseId = klagehendelseId,
                klagebehandlingId = klagebehandlingId,
                opprettet = opprettet,
                sistEndret = sistEndret,
                eksternKlagehendelseId = dto.eventId,
                avsluttetTidspunkt = LocalDateTime.parse(detaljer.avsluttet),
                utfall = detaljer.utfall.toDomene(),
                journalpostreferanser = detaljer.journalpostReferanser.map { JournalpostId(it) },
            )
        }

        KlageinstanshendelseDto.KlageinstanshendelseType.OMGJOERINGSKRAVBEHANDLING_AVSLUTTET -> {
            val detaljer = requireNotNull(dto.detaljer.omgjoeringskravbehandlingAvsluttet) {
                "Mangler omgjoeringskravbehandlingAvsluttet-detaljer for eventId=${dto.eventId}"
            }
            Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet(
                klagehendelseId = klagehendelseId,
                klagebehandlingId = klagebehandlingId,
                opprettet = opprettet,
                sistEndret = sistEndret,
                eksternKlagehendelseId = dto.eventId,
                avsluttetTidspunkt = LocalDateTime.parse(detaljer.avsluttet),
                journalpostreferanser = detaljer.journalpostReferanser.map { JournalpostId(it) },
                utfall = Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet.OmgjøringskravbehandlingAvsluttetUtfall.valueOf(
                    detaljer.utfall,
                ),
            )
        }

        KlageinstanshendelseDto.KlageinstanshendelseType.BEHANDLING_FEILREGISTRERT -> {
            val detaljer = requireNotNull(dto.detaljer.behandlingFeilregistrertDetaljer) {
                "Mangler behandlingFeilregistrertDetaljer for eventId=${dto.eventId}"
            }
            Klageinstanshendelse.BehandlingFeilregistrert(
                klagehendelseId = klagehendelseId,
                klagebehandlingId = klagebehandlingId,
                opprettet = opprettet,
                sistEndret = sistEndret,
                eksternKlagehendelseId = dto.eventId,
                feilregistrertTidspunkt = LocalDateTime.parse(detaljer.feilregistrert),
                årsak = detaljer.reason,
                navIdent = detaljer.navIdent,
                type = detaljer.type.toDomene(),
            )
        }
    }
}

private fun KlageinstanshendelseDto.Utfall.toDomene(): KlagehendelseKlagebehandlingAvsluttetUtfall {
    return when (this) {
        KlageinstanshendelseDto.Utfall.TRUKKET -> KlagehendelseKlagebehandlingAvsluttetUtfall.TRUKKET
        KlageinstanshendelseDto.Utfall.RETUR -> KlagehendelseKlagebehandlingAvsluttetUtfall.RETUR
        KlageinstanshendelseDto.Utfall.OPPHEVET -> KlagehendelseKlagebehandlingAvsluttetUtfall.OPPHEVET
        KlageinstanshendelseDto.Utfall.MEDHOLD -> KlagehendelseKlagebehandlingAvsluttetUtfall.MEDHOLD
        KlageinstanshendelseDto.Utfall.DELVIS_MEDHOLD -> KlagehendelseKlagebehandlingAvsluttetUtfall.DELVIS_MEDHOLD
        KlageinstanshendelseDto.Utfall.STADFESTELSE -> KlagehendelseKlagebehandlingAvsluttetUtfall.STADFESTELSE
        KlageinstanshendelseDto.Utfall.UGUNST -> KlagehendelseKlagebehandlingAvsluttetUtfall.UGUNST
        KlageinstanshendelseDto.Utfall.AVVIST -> KlagehendelseKlagebehandlingAvsluttetUtfall.AVVIST
        KlageinstanshendelseDto.Utfall.HENLAGT -> KlagehendelseKlagebehandlingAvsluttetUtfall.HENLAGT
    }
}

private fun KlageinstanshendelseDto.FeilregistrertType.toDomene(): KlagehendelseFeilregistrertType {
    return when (this) {
        KlageinstanshendelseDto.FeilregistrertType.KLAGE -> KlagehendelseFeilregistrertType.KLAGE
        KlageinstanshendelseDto.FeilregistrertType.ANKE -> KlagehendelseFeilregistrertType.ANKE
        KlageinstanshendelseDto.FeilregistrertType.ANKE_I_TRYGDERETTEN -> KlagehendelseFeilregistrertType.ANKE_I_TRYGDERETTEN
        KlageinstanshendelseDto.FeilregistrertType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET -> KlagehendelseFeilregistrertType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET
        KlageinstanshendelseDto.FeilregistrertType.OMGJOERINGSKRAV -> KlagehendelseFeilregistrertType.OMGJOERINGSKRAV
    }
}
