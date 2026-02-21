package no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse

import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import java.time.LocalDateTime

sealed interface Klageinstanshendelse {

    /** intern id for klagehendelsen */
    val klagehendelseId: KlagehendelseId

    val klagebehandlingId: KlagebehandlingId

    /** tidspunkt for når vi mottok klagehendelsen */
    val opprettet: LocalDateTime

    /** tidspunkt for når klagehendelsen sist ble endret (typisk når vi knytter den til klagebehandlingen). */
    val sistEndret: LocalDateTime

    /** id på hendelsen, eies av klageinstansen */
    val eksternKlagehendelseId: String

    /** tidspunkt for når klagebehandlingen eller omgjøringskravbehandlingen ble avsluttet */
    val avsluttetTidspunkt: LocalDateTime?

    /** Brukes både for avsluttet klagebehandling og avsluttet omgjøringskravbehandling. */
    val journalpostreferanser: List<JournalpostId>

    data class KlagebehandlingAvsluttet(
        override val klagehendelseId: KlagehendelseId,
        override val klagebehandlingId: KlagebehandlingId,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
        override val eksternKlagehendelseId: String,
        override val avsluttetTidspunkt: LocalDateTime,
        val utfall: KlagehendelseKlagebehandlingAvsluttetUtfall,
        override val journalpostreferanser: List<JournalpostId>,

    ) : Klageinstanshendelse {
        enum class KlagehendelseKlagebehandlingAvsluttetUtfall {
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
    }

    data class OmgjøringskravbehandlingAvsluttet(
        override val klagehendelseId: KlagehendelseId,
        override val klagebehandlingId: KlagebehandlingId,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
        override val eksternKlagehendelseId: String,
        override val journalpostreferanser: List<JournalpostId>,
        override val avsluttetTidspunkt: LocalDateTime,
        val utfall: OmgjøringskravbehandlingAvsluttetUtfall,
    ) : Klageinstanshendelse {
        enum class OmgjøringskravbehandlingAvsluttetUtfall {
            MEDHOLD_ETTER_FVL_35,
        }
    }

    data class BehandlingFeilregistrert(
        override val klagehendelseId: KlagehendelseId,
        override val klagebehandlingId: KlagebehandlingId,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
        override val eksternKlagehendelseId: String,
        val feilregistrertTidspunkt: LocalDateTime,
        val årsak: String,
        val navIdent: String,
        val type: KlagehendelseFeilregistrertType,
    ) : Klageinstanshendelse {
        override val journalpostreferanser = emptyList<JournalpostId>()
        override val avsluttetTidspunkt = null

        enum class KlagehendelseFeilregistrertType {
            KLAGE,
            ANKE,
            ANKE_I_TRYGDERETTEN,
            BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET,
            OMGJOERINGSKRAV,
        }
    }
}
