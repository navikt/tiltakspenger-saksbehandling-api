package no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse

import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import java.time.LocalDateTime

sealed interface Klageinstanshendelse {

    /** intern id for klagehendelsen */
    val klagehendelseId: KlagehendelseId

    /** tidspunkt for når vi mottok klagehendelsen */
    val opprettet: LocalDateTime

    /** tidspunkt for når klagehendelsen sist ble endret (typisk når vi knytter den til klabehandlingen). */
    val sistEndret: LocalDateTime

    /** id på hendelsen, eies av klageinstansen */
    val eksternKlagehendelseId: String

    data class KlagebehandlingAvsluttet(
        override val klagehendelseId: KlagehendelseId,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
        override val eksternKlagehendelseId: String,
        val avsluttetTidspunkt: LocalDateTime,
        val utfall: KlagehendelseKlagebehandlingAvsluttetUtfall,
        val journalpostreferanser: List<JournalpostId>,
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

    data class OmgjøringsbehandlingAvsluttet(
        override val klagehendelseId: KlagehendelseId,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
        override val eksternKlagehendelseId: String,
        val kabalReferanse: String,
        val avsluttetTidspunkt: LocalDateTime,
        val journalpostreferanser: List<JournalpostId>,
        val utfall: OmgjøringsbehandlingAvsluttetUtfall,
    ) : Klageinstanshendelse {
        enum class OmgjøringsbehandlingAvsluttetUtfall {
            MEDHOLD_ETTER_FVL_35,
        }
    }

    data class BehandlingFeilregistrert(
        override val klagehendelseId: KlagehendelseId,
        override val opprettet: LocalDateTime,
        override val sistEndret: LocalDateTime,
        override val eksternKlagehendelseId: String,
        val feilregistrertTidspunkt: LocalDateTime,
        val årsak: String,
        val navIdent: String,
        val type: KlagehendelseFeilregistrertType,
    ) : Klageinstanshendelse {
        enum class KlagehendelseFeilregistrertType {
            KLAGE,
            ANKE,
            ANKE_I_TRYGDERETTEN,
            BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET,
            OMGJOERINGSKRAV,
        }
    }
}
