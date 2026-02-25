package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import no.nav.tiltakspenger.saksbehandling.infra.route.AvbruttDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.VentestatusHendelseDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.tilVentestatusHendelseDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAvbruttDTO
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlagehjemmelDto.Companion.toKlagehjemmelDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlageresultatstypeDto.Companion.toKlageresultatstypDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlagestatustypeDto.Companion.toKlagestatustypeDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.formkrav.KlageInnsendingskildeDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.formkrav.KlageInnsendingskildeDto.Companion.toDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.formkrav.KlagefristUnntakSvarordDto
import java.time.LocalDate
import java.time.LocalDateTime

data class KlagebehandlingDTO(
    val id: String,
    val sakId: String,
    val saksnummer: String,
    val fnr: String,
    val opprettet: String,
    val sistEndret: String,
    val iverksattTidspunkt: String?,
    val saksbehandler: String?,
    val journalpostId: String,
    val journalpostOpprettet: String,
    val status: KlagestatustypeDto,
    val resultat: KlageresultatstypeDto?,
    val vedtakDetKlagesPå: String?,
    val erKlagerPartISaken: Boolean,
    val klagesDetPåKonkreteElementerIVedtaket: Boolean,
    val erKlagefristenOverholdt: Boolean,
    val erUnntakForKlagefrist: KlagefristUnntakSvarordDto?,
    val erKlagenSignert: Boolean,
    val innsendingsdato: LocalDate,
    val innsendingskilde: KlageInnsendingskildeDto,
    val brevtekst: List<TittelOgTekstDTO>,
    val avbrutt: AvbruttDTO?,
    val kanIverksetteVedtak: Boolean?,
    val kanIverksetteOpprettholdelse: Boolean,
    val årsak: String?,
    val hjemler: List<KlagehjemmelDto>,
    val begrunnelse: String?,
    val rammebehandlingId: String?,
    val ventestatus: VentestatusHendelseDTO?,
    val iverksattOpprettholdelseTidspunkt: LocalDateTime?,
    val journalføringstidspunktInnstillingsbrev: LocalDateTime?,
    val distribusjonstidspunktInnstillingsbrev: LocalDateTime?,
    val oversendtKlageinstansenTidspunkt: LocalDateTime?,
    val klageinstanshendelser: List<KlageinstanshendelseDTO>,
) {
    data class TittelOgTekstDTO(
        val tittel: String,
        val tekst: String,
    )
}

fun Klagebehandling.tilKlagebehandlingDTO() = KlagebehandlingDTO(
    id = id.toString(),
    sakId = sakId.toString(),
    saksnummer = saksnummer.toString(),
    fnr = fnr.verdi,
    opprettet = opprettet.toString(),
    sistEndret = sistEndret.toString(),
    iverksattTidspunkt = iverksattTidspunkt?.toString(),
    saksbehandler = saksbehandler,
    journalpostId = klagensJournalpostId.toString(),
    journalpostOpprettet = klagensJournalpostOpprettet.toString(),
    status = status.toKlagestatustypeDto(),
    resultat = resultat?.toKlageresultatstypDto(),
    vedtakDetKlagesPå = formkrav.vedtakDetKlagesPå?.toString(),
    erKlagerPartISaken = formkrav.erKlagerPartISaken,
    klagesDetPåKonkreteElementerIVedtaket = formkrav.klagesDetPåKonkreteElementerIVedtaket,
    erKlagefristenOverholdt = formkrav.erKlagefristenOverholdt,
    erUnntakForKlagefrist = when (formkrav.erUnntakForKlagefrist) {
        null -> null
        KlagefristUnntakSvarord.JA_AV_SÆRLIGE_GRUNNER -> KlagefristUnntakSvarordDto.JA_AV_SÆRLIGE_GRUNNER
        KlagefristUnntakSvarord.JA_KLAGER_KAN_IKKE_LASTES_FOR_Å_HA_SENDT_INN_ETTER_FRISTEN -> KlagefristUnntakSvarordDto.JA_KLAGER_KAN_IKKE_LASTES_FOR_Å_HA_SENDT_INN_ETTER_FRISTEN
        KlagefristUnntakSvarord.NEI -> KlagefristUnntakSvarordDto.NEI
    },
    erKlagenSignert = formkrav.erKlagenSignert,
    brevtekst = brevtekst?.map {
        KlagebehandlingDTO.TittelOgTekstDTO(
            tittel = it.tittel.value,
            tekst = it.tekst.value,
        )
    } ?: emptyList(),
    avbrutt = this.avbrutt?.toAvbruttDTO(),
    kanIverksetteVedtak = kanIverksetteVedtak,
    kanIverksetteOpprettholdelse = kanIverksetteOpprettholdelse,
    årsak = (resultat as? Klagebehandlingsresultat.Omgjør)?.årsak?.name,
    begrunnelse = (resultat as? Klagebehandlingsresultat.Omgjør)?.begrunnelse?.verdi,
    rammebehandlingId = (resultat as? Klagebehandlingsresultat.Omgjør)?.rammebehandlingId?.toString(),
    ventestatus = ventestatus.ventestatusHendelser.lastOrNull()?.tilVentestatusHendelseDTO(),
    hjemler = (resultat as? Klagebehandlingsresultat.Opprettholdt)?.hjemler?.toKlagehjemmelDto() ?: emptyList(),
    innsendingsdato = this.formkrav.innsendingsdato,
    innsendingskilde = this.formkrav.innsendingskilde.toDto(),
    iverksattOpprettholdelseTidspunkt = (resultat as? Klagebehandlingsresultat.Opprettholdt)?.iverksattOpprettholdelseTidspunkt,
    journalføringstidspunktInnstillingsbrev = (resultat as? Klagebehandlingsresultat.Opprettholdt)?.journalføringstidspunktInnstillingsbrev,
    distribusjonstidspunktInnstillingsbrev = (resultat as? Klagebehandlingsresultat.Opprettholdt)?.distribusjonstidspunktInnstillingsbrev,
    oversendtKlageinstansenTidspunkt = (resultat as? Klagebehandlingsresultat.Opprettholdt)?.oversendtKlageinstansenTidspunkt,
    klageinstanshendelser = (resultat as? Klagebehandlingsresultat.Opprettholdt)?.klageinstanshendelser?.toDTO() ?: emptyList(),
)
