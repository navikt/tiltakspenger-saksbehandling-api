package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Avvist
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Omgjør
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Opprettholdt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klageinstanshendelser
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.KlagehendelseId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.BehandlingFeilregistrert.KlagehendelseFeilregistrertType
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.OmgjøringsbehandlingAvsluttet.OmgjøringsbehandlingAvsluttetUtfall
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingsresultatDbJson.KlagebehandlingsresultatDbEnum
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagehjemmelDb.Companion.toDb
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagehjemmelDb.Companion.toDomain
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import java.time.LocalDate
import java.time.LocalDateTime

private data class KlagebehandlingsresultatDbJson(
    val type: KlagebehandlingsresultatDbEnum,
    val omgjørBegrunnelse: String?,
    val omgjørÅrsak: KlagebehandlingsOmgjørÅrsakDbEnum?,
    val rammebehandlingId: String?,
    val hjemler: List<KlagehjemmelDb>?,
    val iverksattOpprettholdelseTidspunkt: LocalDateTime?,
    val brevdato: LocalDate?,
    val oversendtKlageinstansenTidspunkt: LocalDateTime?,
    val journalpostIdInnstillingsbrev: String?,
    val journalføringstidspunktInnstillingsbrev: LocalDateTime?,
    val distribusjonIdInnstillingsbrev: String?,
    val distribusjonstidspunktInnstillingsbrev: LocalDateTime?,
    val klageinsttansehendelser: List<KlageinstanshendelseDb>,

    // TODO jah: Flytt avvisningsbrevtekst hit fra klagebehandlingstabellen
) {
    enum class KlagebehandlingsresultatDbEnum {
        AVVIST,
        OMGJØR,
        OPPRETTHOLDT,
    }

    data class KlageinstanshendelseDb(
        val klagehendelseId: String,
        val opprettet: LocalDateTime,
        val sistEndret: LocalDateTime,
        val eksternKlagehendelseId: String,
        val type: TypeDb,

        val avsluttetUtfall: AvsluttetUtfallDb?,
        // avsluttetTidspunkt+journalpostreferanser brukes både av avsluttet klagebehandling og omgjøringskravbehandling
        val avsluttetTidspunkt: LocalDateTime?,
        val journalpostreferanser: List<String>,

        val omgjøringskravKabalReferanse: String?,
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

            fun toDomain(): OmgjøringsbehandlingAvsluttetUtfall {
                return when (this) {
                    MEDHOLD_ETTER_FVL_35 -> OmgjøringsbehandlingAvsluttetUtfall.MEDHOLD_ETTER_FVL_35
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
                    opprettet = opprettet,
                    sistEndret = sistEndret,
                    eksternKlagehendelseId = eksternKlagehendelseId,
                    avsluttetTidspunkt = avsluttetTidspunkt!!,
                    utfall = avsluttetUtfall!!.toDomain(),
                    journalpostreferanser = journalpostreferanser.map { JournalpostId(it) },
                )

                TypeDb.OMGJOERINGSKRAVBEHANDLING_AVSLUTTET -> Klageinstanshendelse.OmgjøringsbehandlingAvsluttet(
                    klagehendelseId = KlagehendelseId.fromString(klagehendelseId),
                    opprettet = opprettet,
                    sistEndret = sistEndret,
                    eksternKlagehendelseId = eksternKlagehendelseId,
                    kabalReferanse = omgjøringskravKabalReferanse!!,
                    avsluttetTidspunkt = avsluttetTidspunkt!!,
                    utfall = omgjøringskravUtfall!!.toDomain(),
                    journalpostreferanser = journalpostreferanser.map { JournalpostId(it) },
                )

                TypeDb.BEHANDLING_FEILREGISTRERT -> Klageinstanshendelse.BehandlingFeilregistrert(
                    klagehendelseId = KlagehendelseId.fromString(klagehendelseId),
                    opprettet = opprettet,
                    sistEndret = sistEndret,
                    eksternKlagehendelseId = eksternKlagehendelseId,
                    feilregistrertTidspunkt = feilregistrertTidspunkt!!,
                    årsak = feilregistrertÅrsak!!,
                    navIdent = feilregistrertNavIdent!!,
                    type = feilregistrertType!!.toDomain(),
                )

                else -> throw IllegalArgumentException("Ukjent klageinstanshendelse type: $type")
            }
        }
    }

    fun toDomain(
        brevtekst: Brevtekster?,
    ): Klagebehandlingsresultat {
        return when (type) {
            KlagebehandlingsresultatDbEnum.AVVIST -> Avvist(
                brevtekst = brevtekst,
            )

            KlagebehandlingsresultatDbEnum.OMGJØR -> Omgjør(
                årsak = omgjørÅrsak!!.toDomain(),
                begrunnelse = Begrunnelse.create(omgjørBegrunnelse!!)!!,
                rammebehandlingId = rammebehandlingId?.let { BehandlingId.fromString(it) },
            )

            KlagebehandlingsresultatDbEnum.OPPRETTHOLDT -> Opprettholdt(
                hjemler = hjemler!!.toDomain(),
                brevtekst = brevtekst,
                brevdato = brevdato,
                iverksattOpprettholdelseTidspunkt = iverksattOpprettholdelseTidspunkt,
                journalpostIdInnstillingsbrev = journalpostIdInnstillingsbrev?.let { JournalpostId(it) },
                journalføringstidspunktInnstillingsbrev = journalføringstidspunktInnstillingsbrev,
                distribusjonIdInnstillingsbrev = distribusjonIdInnstillingsbrev?.let { DistribusjonId(it) },
                distribusjonstidspunktInnstillingsbrev = distribusjonstidspunktInnstillingsbrev,
                oversendtKlageinstansenTidspunkt = oversendtKlageinstansenTidspunkt,
                klageinsttansehendelser = Klageinstanshendelser(klageinsttansehendelser.map { it.toDomain() }),
            )
        }
    }
}

fun Klagebehandlingsresultat.toDbJson(): String {
    return KlagebehandlingsresultatDbJson(
        type = when (this) {
            is Avvist -> KlagebehandlingsresultatDbEnum.AVVIST
            is Omgjør -> KlagebehandlingsresultatDbEnum.OMGJØR
            is Opprettholdt -> KlagebehandlingsresultatDbEnum.OPPRETTHOLDT
        },
        omgjørBegrunnelse = (this as? Omgjør)?.begrunnelse?.verdi,
        omgjørÅrsak = (this as? Omgjør)?.årsak?.toDbEnum(),
        rammebehandlingId = (this as? Omgjør)?.rammebehandlingId?.toString(),
        hjemler = (this as? Opprettholdt)?.hjemler?.map { it.toDb() },
        iverksattOpprettholdelseTidspunkt = (this as? Opprettholdt)?.iverksattOpprettholdelseTidspunkt,
        brevdato = (this as? Opprettholdt)?.brevdato,
        oversendtKlageinstansenTidspunkt = (this as? Opprettholdt)?.oversendtKlageinstansenTidspunkt,
        journalpostIdInnstillingsbrev = (this as? Opprettholdt)?.journalpostIdInnstillingsbrev?.toString(),
        journalføringstidspunktInnstillingsbrev = (this as? Opprettholdt)?.journalføringstidspunktInnstillingsbrev,
        distribusjonIdInnstillingsbrev = (this as? Opprettholdt)?.distribusjonIdInnstillingsbrev?.toString(),
        distribusjonstidspunktInnstillingsbrev = (this as? Opprettholdt)?.distribusjonstidspunktInnstillingsbrev,
        klageinsttansehendelser = (this as? Opprettholdt)?.klageinsttansehendelser.toDb(),
    ).let { serialize(it) }
}

fun String.toKlagebehandlingResultat(brevtekst: Brevtekster?): Klagebehandlingsresultat {
    return deserialize<KlagebehandlingsresultatDbJson>(this).toDomain(brevtekst)
}
