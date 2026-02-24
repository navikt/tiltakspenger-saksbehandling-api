package no.nav.tiltakspenger.saksbehandling.klage.infra.kafka

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.KlagehendelseId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.NyKlagehendelse
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagehendelseRepo
import java.time.Clock
import java.util.UUID

class LokalKabalHendelseService(
    private val klagePostgresRepo: KlagebehandlingRepo,
    private val klagehendelsePostgresRepo: KlagehendelseRepo,
    private val clock: Clock,
) {

    fun genererHendelse(command: GenererLokalKabalHendelseCommand) {
        val klagebehandling = klagePostgresRepo.hentForKlagebehandlingId(command.klagebehandlingId)
            ?: error("Fant ingen klagebehandling med id ${command.klagebehandlingId}")

        val manuellHendelse = when (command.type) {
            "KLAGEBEHANDLING_AVSLUTTET" -> GenerererKlageinstanshendelse.avsluttetJson(
                eventId = UUID.randomUUID().toString(),
                kildeReferanse = klagebehandling.id.toString(),
                avsluttetTidspunkt = nå(clock).toString(),
                utfall = command.utfall.toAvsluttetUtfall(),
                journalpostReferanser = emptyList(),
            )

            "OMGJOERINGSKRAVBEHANDLING_AVSLUTTET" -> GenerererKlageinstanshendelse.omgjøringskravbehandlingAvsluttet(
                eventId = UUID.randomUUID().toString(),
                kildeReferanse = klagebehandling.id.toString(),
                avsluttetTidspunkt = nå(clock).toString(),
                utfall = command.utfall.toOmgjoeringskravAvsluttetUtfall(),
                journalpostReferanser = emptyList(),
            )

            "BEHANDLING_FEILREGISTRERT" -> GenerererKlageinstanshendelse.behandlingFeilregistrert(
                eventId = UUID.randomUUID().toString(),
                kildeReferanse = klagebehandling.id.toString(),
                navIdent = "TEST",
                reason = "Feilregistrert i test",
                type = command.utfall.toFeilregistrertType(),
            )

            else -> error("Ukjent type: ${command.type}")
        }

        val nyHendelse = NyKlagehendelse(
            klagehendelseId = KlagehendelseId.random(),
            opprettet = nå(clock),
            sistEndret = nå(clock),
            eksternKlagehendelseId = "lokal-kabal-${command.type}-${command.utfall}-${command.klagebehandlingId}",
            key = "lokal-kabal-${command.type}-${command.utfall}-${command.klagebehandlingId}",
            value = manuellHendelse,
            sakId = klagebehandling.sakId,
            klagebehandlingId = klagebehandling.id,
        )

        klagehendelsePostgresRepo.lagreNyHendelse(nyHendelse)
    }
}

data class GenererLokalKabalHendelseCommand(
    val type: String,
    val utfall: LokalHendelseUtfall,
    val klagebehandlingId: KlagebehandlingId,
)

enum class LokalHendelseUtfall {
    TRUKKET,
    RETUR,
    OPPHEVET,
    MEDHOLD,
    DELVIS_MEDHOLD,
    STADFESTELSE,
    UGUNST,
    AVVIST,
    HENLAGT,

    MEDHOLD_ETTER_FVL_35,

    KLAGE,
    ANKE,
    ANKE_I_TRYGDERETTEN,
    BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET,
    OMGJOERINGSKRAV,
    ;

    fun toAvsluttetUtfall(): Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall =
        when (this) {
            TRUKKET -> Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.TRUKKET

            RETUR -> Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.RETUR

            OPPHEVET -> Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.OPPHEVET

            MEDHOLD -> Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.MEDHOLD

            DELVIS_MEDHOLD -> Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.DELVIS_MEDHOLD

            STADFESTELSE -> Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.STADFESTELSE

            UGUNST -> Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.UGUNST

            AVVIST -> Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.AVVIST

            HENLAGT -> Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.HENLAGT

            MEDHOLD_ETTER_FVL_35,
            KLAGE,
            ANKE,
            ANKE_I_TRYGDERETTEN,
            BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET,
            OMGJOERINGSKRAV,
            -> error("Utfall $this kan ikke konverteres til KlagebehandlingAvsluttetUtfall")
        }

    fun toOmgjoeringskravAvsluttetUtfall(): Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet.OmgjøringskravbehandlingAvsluttetUtfall =
        when (this) {
            MEDHOLD_ETTER_FVL_35 -> Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet.OmgjøringskravbehandlingAvsluttetUtfall.MEDHOLD_ETTER_FVL_35
            else -> error("Utfall $this kan ikke konverteres til OmgjoeringskravbehandlingAvsluttetUtfall")
        }

    fun toFeilregistrertType(): Klageinstanshendelse.BehandlingFeilregistrert.KlagehendelseFeilregistrertType =
        when (this) {
            KLAGE -> Klageinstanshendelse.BehandlingFeilregistrert.KlagehendelseFeilregistrertType.KLAGE
            ANKE -> Klageinstanshendelse.BehandlingFeilregistrert.KlagehendelseFeilregistrertType.ANKE
            ANKE_I_TRYGDERETTEN -> Klageinstanshendelse.BehandlingFeilregistrert.KlagehendelseFeilregistrertType.ANKE_I_TRYGDERETTEN
            BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET -> Klageinstanshendelse.BehandlingFeilregistrert.KlagehendelseFeilregistrertType.BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET
            OMGJOERINGSKRAV -> Klageinstanshendelse.BehandlingFeilregistrert.KlagehendelseFeilregistrertType.OMGJOERINGSKRAV
            else -> error("Utfall $this kan ikke konverteres til FeilregistrertType")
        }
}
