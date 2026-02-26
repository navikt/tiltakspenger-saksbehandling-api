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
    val klageinstanshendelser: List<KlageinstanshendelseDb>,
    val ferdigstiltTidspunkt: LocalDateTime?,

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
                klageinstanshendelser = Klageinstanshendelser(klageinstanshendelser.map { it.toDomain() }),
                ferdigstiltTidspunkt = ferdigstiltTidspunkt,
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
        klageinstanshendelser = (this as? Opprettholdt)?.klageinstanshendelser?.toDb() ?: emptyList(),
        ferdigstiltTidspunkt = (this as? Opprettholdt)?.ferdigstiltTidspunkt,
    ).let { serialize(it) }
}

fun String.toKlagebehandlingResultat(brevtekst: Brevtekster?): Klagebehandlingsresultat {
    return deserialize<KlagebehandlingsresultatDbJson>(this).toDomain(brevtekst)
}
