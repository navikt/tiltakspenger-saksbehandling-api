package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klageinstanshendelser
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.KlagehendelseId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.BehandlingFeilregistrert.KlagehendelseFeilregistrertType
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet.OmgjøringskravbehandlingAvsluttetUtfall
import java.time.LocalDateTime

data class KlageinstanshendelseDb(
    val klagehendelseId: String,
    val klagebehandlingId: String,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val eksternKlagehendelseId: String,
    val type: TypeDb,

    val avsluttetUtfall: AvsluttetUtfallDb?,
    // avsluttetTidspunkt+journalpostreferanser brukes både av avsluttet klagebehandling og omgjøringskravbehandling
    val avsluttetTidspunkt: LocalDateTime?,
    val journalpostreferanser: List<String>,

    val omgjøringskravUtfall: OmgjøringsUtfallDb?,

    val feilregistrertTidspunkt: LocalDateTime?,
    val feilregistrertÅrsak: String?,
    val feilregistrertNavIdent: String?,
    val feilregistrertType: FeilregistrertTypeDb?,
) {
    enum class FeilregistrertTypeDb {
        KLAGE,
        ANKE,
        ANKE_I_TRYGDERETTEN,
        BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET,
        OMGJOERINGSKRAV,
        ;

        fun toDomain(): KlagehendelseFeilregistrertType {
            return when (this) {
                KLAGE -> KlagehendelseFeilregistrertType.KLAGE
                ANKE -> KlagehendelseFeilregistrertType.ANKE
                ANKE_I_TRYGDERETTEN -> KlagehendelseFeilregistrertType.ANKE_I_TRYGDERETTEN
                BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET -> KlagehendelseFeilregistrertType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET
                OMGJOERINGSKRAV -> KlagehendelseFeilregistrertType.OMGJOERINGSKRAV
            }
        }
    }

    enum class TypeDb {
        KLAGEBEHANDLING_AVSLUTTET,
        OMGJOERINGSKRAVBEHANDLING_AVSLUTTET,
        BEHANDLING_FEILREGISTRERT,
    }

    enum class OmgjøringsUtfallDb {
        MEDHOLD_ETTER_FVL_35,
        ;

        fun toDomain(): OmgjøringskravbehandlingAvsluttetUtfall {
            return when (this) {
                MEDHOLD_ETTER_FVL_35 -> OmgjøringskravbehandlingAvsluttetUtfall.MEDHOLD_ETTER_FVL_35
            }
        }
    }

    enum class AvsluttetUtfallDb {
        TRUKKET,
        RETUR,
        OPPHEVET,
        MEDHOLD,
        DELVIS_MEDHOLD,
        STADFESTELSE,
        UGUNST,
        AVVIST,
        HENLAGT,
        ;

        fun toDomain(): KlagehendelseKlagebehandlingAvsluttetUtfall {
            return when (this) {
                TRUKKET -> KlagehendelseKlagebehandlingAvsluttetUtfall.TRUKKET
                RETUR -> KlagehendelseKlagebehandlingAvsluttetUtfall.RETUR
                OPPHEVET -> KlagehendelseKlagebehandlingAvsluttetUtfall.OPPHEVET
                MEDHOLD -> KlagehendelseKlagebehandlingAvsluttetUtfall.MEDHOLD
                DELVIS_MEDHOLD -> KlagehendelseKlagebehandlingAvsluttetUtfall.DELVIS_MEDHOLD
                STADFESTELSE -> KlagehendelseKlagebehandlingAvsluttetUtfall.STADFESTELSE
                UGUNST -> KlagehendelseKlagebehandlingAvsluttetUtfall.UGUNST
                AVVIST -> KlagehendelseKlagebehandlingAvsluttetUtfall.AVVIST
                HENLAGT -> KlagehendelseKlagebehandlingAvsluttetUtfall.HENLAGT
            }
        }
    }

    fun toDomain(): Klageinstanshendelse {
        return when (type) {
            TypeDb.KLAGEBEHANDLING_AVSLUTTET -> Klageinstanshendelse.KlagebehandlingAvsluttet(
                klagehendelseId = KlagehendelseId.fromString(klagehendelseId),
                klagebehandlingId = KlagebehandlingId.fromString(klagebehandlingId),
                opprettet = opprettet,
                sistEndret = sistEndret,
                eksternKlagehendelseId = eksternKlagehendelseId,
                avsluttetTidspunkt = avsluttetTidspunkt!!,
                utfall = avsluttetUtfall!!.toDomain(),
                journalpostreferanser = journalpostreferanser.map { JournalpostId(it) },
            )

            TypeDb.OMGJOERINGSKRAVBEHANDLING_AVSLUTTET -> Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet(
                klagehendelseId = KlagehendelseId.fromString(klagehendelseId),
                klagebehandlingId = KlagebehandlingId.fromString(klagebehandlingId),
                opprettet = opprettet,
                sistEndret = sistEndret,
                eksternKlagehendelseId = eksternKlagehendelseId,
                avsluttetTidspunkt = avsluttetTidspunkt!!,
                utfall = omgjøringskravUtfall!!.toDomain(),
                journalpostreferanser = journalpostreferanser.map { JournalpostId(it) },
            )

            TypeDb.BEHANDLING_FEILREGISTRERT -> Klageinstanshendelse.BehandlingFeilregistrert(
                klagehendelseId = KlagehendelseId.fromString(klagehendelseId),
                klagebehandlingId = KlagebehandlingId.fromString(klagebehandlingId),
                opprettet = opprettet,
                sistEndret = sistEndret,
                eksternKlagehendelseId = eksternKlagehendelseId,
                feilregistrertTidspunkt = feilregistrertTidspunkt!!,
                årsak = feilregistrertÅrsak!!,
                navIdent = feilregistrertNavIdent!!,
                type = feilregistrertType!!.toDomain(),
            )
        }
    }
}

fun Klageinstanshendelser.toDb(): List<KlageinstanshendelseDb> {
    return this.map { klageinstanshendelse ->
        KlageinstanshendelseDb(
            klagehendelseId = klageinstanshendelse.klagehendelseId.toString(),
            klagebehandlingId = klageinstanshendelse.klagebehandlingId.toString(),
            opprettet = klageinstanshendelse.opprettet,
            sistEndret = klageinstanshendelse.sistEndret,
            eksternKlagehendelseId = klageinstanshendelse.eksternKlagehendelseId,
            type = when (klageinstanshendelse) {
                is Klageinstanshendelse.KlagebehandlingAvsluttet -> KlageinstanshendelseDb.TypeDb.KLAGEBEHANDLING_AVSLUTTET
                is Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet -> KlageinstanshendelseDb.TypeDb.OMGJOERINGSKRAVBEHANDLING_AVSLUTTET
                is Klageinstanshendelse.BehandlingFeilregistrert -> KlageinstanshendelseDb.TypeDb.BEHANDLING_FEILREGISTRERT
            },
            avsluttetUtfall = (klageinstanshendelse as? Klageinstanshendelse.KlagebehandlingAvsluttet)?.utfall?.let {
                when (it) {
                    KlagehendelseKlagebehandlingAvsluttetUtfall.TRUKKET -> KlageinstanshendelseDb.AvsluttetUtfallDb.TRUKKET
                    KlagehendelseKlagebehandlingAvsluttetUtfall.RETUR -> KlageinstanshendelseDb.AvsluttetUtfallDb.RETUR
                    KlagehendelseKlagebehandlingAvsluttetUtfall.OPPHEVET -> KlageinstanshendelseDb.AvsluttetUtfallDb.OPPHEVET
                    KlagehendelseKlagebehandlingAvsluttetUtfall.MEDHOLD -> KlageinstanshendelseDb.AvsluttetUtfallDb.MEDHOLD
                    KlagehendelseKlagebehandlingAvsluttetUtfall.DELVIS_MEDHOLD -> KlageinstanshendelseDb.AvsluttetUtfallDb.DELVIS_MEDHOLD
                    KlagehendelseKlagebehandlingAvsluttetUtfall.STADFESTELSE -> KlageinstanshendelseDb.AvsluttetUtfallDb.STADFESTELSE
                    KlagehendelseKlagebehandlingAvsluttetUtfall.UGUNST -> KlageinstanshendelseDb.AvsluttetUtfallDb.UGUNST
                    KlagehendelseKlagebehandlingAvsluttetUtfall.AVVIST -> KlageinstanshendelseDb.AvsluttetUtfallDb.AVVIST
                    KlagehendelseKlagebehandlingAvsluttetUtfall.HENLAGT -> KlageinstanshendelseDb.AvsluttetUtfallDb.HENLAGT
                }
            },
            avsluttetTidspunkt = klageinstanshendelse.avsluttetTidspunkt,
            journalpostreferanser = klageinstanshendelse.journalpostreferanser.map { it.toString() },
            omgjøringskravUtfall = (klageinstanshendelse as? Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet)?.utfall?.let {
                when (it) {
                    OmgjøringskravbehandlingAvsluttetUtfall.MEDHOLD_ETTER_FVL_35 -> KlageinstanshendelseDb.OmgjøringsUtfallDb.MEDHOLD_ETTER_FVL_35
                }
            },
            feilregistrertTidspunkt = (klageinstanshendelse as? Klageinstanshendelse.BehandlingFeilregistrert)?.feilregistrertTidspunkt,
            feilregistrertÅrsak = (klageinstanshendelse as? Klageinstanshendelse.BehandlingFeilregistrert)?.årsak,
            feilregistrertNavIdent = (klageinstanshendelse as? Klageinstanshendelse.BehandlingFeilregistrert)?.navIdent,
            feilregistrertType = (klageinstanshendelse as? Klageinstanshendelse.BehandlingFeilregistrert)?.type?.let {
                when (it) {
                    KlagehendelseFeilregistrertType.KLAGE -> KlageinstanshendelseDb.FeilregistrertTypeDb.KLAGE
                    KlagehendelseFeilregistrertType.ANKE -> KlageinstanshendelseDb.FeilregistrertTypeDb.ANKE
                    KlagehendelseFeilregistrertType.ANKE_I_TRYGDERETTEN -> KlageinstanshendelseDb.FeilregistrertTypeDb.ANKE_I_TRYGDERETTEN
                    KlagehendelseFeilregistrertType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET -> KlageinstanshendelseDb.FeilregistrertTypeDb.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET
                    KlagehendelseFeilregistrertType.OMGJOERINGSKRAV -> KlageinstanshendelseDb.FeilregistrertTypeDb.OMGJOERINGSKRAV
                }
            },
        )
    }
}
