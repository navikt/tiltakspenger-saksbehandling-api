package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse.Companion.toBegrunnelse
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalpost.DokumentInfoId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Avvist
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Omgjør
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Opprettholdt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klageinstanshendelser
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagebehandlingsresultatDbJson.KlagebehandlingsresultatDbEnum
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagehjemmelDb.Companion.toDb
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagehjemmelDb.Companion.toDomain
import java.time.LocalDate
import java.time.LocalDateTime

private data class KlagebehandlingsresultatDbJson(
    val type: KlagebehandlingsresultatDbEnum,
    val omgjørBegrunnelse: String?,
    val omgjørÅrsak: KlagebehandlingsOmgjørÅrsakDbEnum?,
    val rammebehandlingId: List<String>,
    val åpenRammebehandlingId: String?,
    val hjemler: List<KlagehjemmelDb>?,
    val iverksattOpprettholdelseTidspunkt: LocalDateTime?,
    val brevdato: LocalDate?,
    val oversendtKlageinstansenTidspunkt: LocalDateTime?,
    val journalpostIdInnstillingsbrev: String?,
    // TODO jah: Kan fjerne nullable når vi har verifisert at databasen ikke har null her eller vi har skrevet et migreringsskript.
    val dokumentInfoIder: List<String>? = emptyList(),
    val journalføringstidspunktInnstillingsbrev: LocalDateTime?,
    val distribusjonIdInnstillingsbrev: String?,
    val distribusjonstidspunktInnstillingsbrev: LocalDateTime?,
    val klageinstanshendelser: List<KlageinstanshendelseDb>,
    val ferdigstiltTidspunkt: LocalDateTime?,
    // kan fjerne default etter migrering
    val begrunnelseFerdigstilling: String? = null,

    // TODO jah: Flytt avvisningsbrevtekst hit fra klagebehandlingstabellen
) {
    enum class KlagebehandlingsresultatDbEnum {
        AVVIST,
        OMGJØR,
        OPPRETTHOLDT,
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
                rammebehandlingId = rammebehandlingId.map { BehandlingId.fromString(it) },
                åpenRammebehandlingId = åpenRammebehandlingId?.let { BehandlingId.fromString(it) },
                ferdigstiltTidspunkt = ferdigstiltTidspunkt,
                begrunnelseFerdigstilling = begrunnelseFerdigstilling?.toBegrunnelse(),
            )

            KlagebehandlingsresultatDbEnum.OPPRETTHOLDT -> Opprettholdt(
                hjemler = hjemler!!.toDomain(),
                brevtekst = brevtekst,
                brevdato = brevdato,
                iverksattOpprettholdelseTidspunkt = iverksattOpprettholdelseTidspunkt,
                journalpostIdInnstillingsbrev = journalpostIdInnstillingsbrev?.let { JournalpostId(it) },
                dokumentInfoIder = dokumentInfoIder?.map { DokumentInfoId(it) } ?: emptyList(),
                journalføringstidspunktInnstillingsbrev = journalføringstidspunktInnstillingsbrev,
                distribusjonIdInnstillingsbrev = distribusjonIdInnstillingsbrev?.let { DistribusjonId(it) },
                distribusjonstidspunktInnstillingsbrev = distribusjonstidspunktInnstillingsbrev,
                oversendtKlageinstansenTidspunkt = oversendtKlageinstansenTidspunkt,
                klageinstanshendelser = Klageinstanshendelser(klageinstanshendelser.map { it.toDomain() }),
                ferdigstiltTidspunkt = ferdigstiltTidspunkt,
                rammebehandlingId = rammebehandlingId.map { BehandlingId.fromString(it) },
                begrunnelseFerdigstilling = begrunnelseFerdigstilling?.toBegrunnelse(),
                åpenRammebehandlingId = åpenRammebehandlingId?.let { BehandlingId.fromString(it) },
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
        rammebehandlingId = this.rammebehandlingId.map { it.toString() },
        hjemler = (this as? Opprettholdt)?.hjemler?.map { it.toDb() },
        iverksattOpprettholdelseTidspunkt = (this as? Opprettholdt)?.iverksattOpprettholdelseTidspunkt,
        brevdato = (this as? Opprettholdt)?.brevdato,
        oversendtKlageinstansenTidspunkt = (this as? Opprettholdt)?.oversendtKlageinstansenTidspunkt,
        journalpostIdInnstillingsbrev = (this as? Opprettholdt)?.journalpostIdInnstillingsbrev?.toString(),
        dokumentInfoIder = (this as? Opprettholdt)?.dokumentInfoIder?.map { it.toString() } ?: emptyList(),
        journalføringstidspunktInnstillingsbrev = (this as? Opprettholdt)?.journalføringstidspunktInnstillingsbrev,
        distribusjonIdInnstillingsbrev = (this as? Opprettholdt)?.distribusjonIdInnstillingsbrev?.toString(),
        distribusjonstidspunktInnstillingsbrev = (this as? Opprettholdt)?.distribusjonstidspunktInnstillingsbrev,
        klageinstanshendelser = (this as? Opprettholdt)?.klageinstanshendelser?.toDb() ?: emptyList(),
        ferdigstiltTidspunkt = this.ferdigstiltTidspunkt,
        begrunnelseFerdigstilling = this.begrunnelseFerdigstilling?.verdi,
        åpenRammebehandlingId = this.åpenRammebehandlingId?.toString(),
    ).let { serialize(it) }
}

fun String.toKlagebehandlingResultat(brevtekst: Brevtekster?): Klagebehandlingsresultat {
    return deserialize<KlagebehandlingsresultatDbJson>(this).toDomain(brevtekst)
}
