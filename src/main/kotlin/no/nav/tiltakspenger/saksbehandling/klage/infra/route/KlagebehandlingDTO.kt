package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import no.nav.tiltakspenger.saksbehandling.dokument.TittelOgTekstDTO
import no.nav.tiltakspenger.saksbehandling.dokument.toDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.AvbruttDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.VentestatusHendelseDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.tilVentestatusHendelseDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAvbruttDTO
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlageFormkrav
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlagehjemmelDto.Companion.toKlagehjemmelDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlagestatustypeDto.Companion.toKlagestatustypeDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.formkrav.KlageInnsendingskildeDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.formkrav.KlageInnsendingskildeDto.Companion.toDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.formkrav.KlagefristUnntakSvarordDto
import java.time.LocalDateTime

data class KlagebehandlingDTO(
    val id: String,
    val sakId: String,
    val saksnummer: String,
    val fnr: String,
    val opprettet: String,
    val sistEndret: String,
    /** Brukes av [Klagebehandlingsresultat.Omgjør] og [Klagebehandlingsresultat.Avvist] */
    val iverksattTidspunkt: String?,
    /** Vil være null i tilstanden [no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.KLAR_TIL_BEHANDLING]*/
    val saksbehandler: String?,
    val klagensJournalpostId: String,
    val klagensJournalpostOpprettet: String,
    val status: KlagestatustypeDto,
    /** Vil være null frem til enten formkravene ikke er oppfylt eller saksbehandler har valgt opprettholdelse eller omgjøring (medhold)*/
    val resultat: KlagebehandlingsresultatDTO?,
    /** Vil være null frem til den er avbrutt og få tilstanden [no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus.AVBRUTT]*/
    val avbrutt: AvbruttDTO?,
    val kanIverksetteVedtak: Boolean?,
    val kanIverksetteOpprettholdelse: Boolean,
    /** Vil være null mens den ikke er på vent. */
    val ventestatus: VentestatusHendelseDTO?,
    val formkrav: KlageFormkravDTO,
)

data class KlageFormkravDTO(
    /** Vil være null hvis bruker ikke har klaget på et vedtak. Formkravene vil ikke være oppfylt*/
    val vedtakDetKlagesPå: String?,
    val erKlagerPartISaken: Boolean,
    val klagesDetPåKonkreteElementerIVedtaket: Boolean,
    val erKlagefristenOverholdt: Boolean,
    /** Er null hvis [erKlagefristenOverholdt] er true, ellers utfylt. */
    val erUnntakForKlagefrist: KlagefristUnntakSvarordDto?,
    val erKlagenSignert: Boolean,
    val innsendingsdato: String,
    val innsendingskilde: KlageInnsendingskildeDto,
) {
    init {
        require(erKlagefristenOverholdt == (erUnntakForKlagefrist == null))
    }
}

fun KlageFormkrav.toDTO(): KlageFormkravDTO = KlageFormkravDTO(
    vedtakDetKlagesPå = vedtakDetKlagesPå?.toString(),
    erKlagerPartISaken = erKlagerPartISaken,
    klagesDetPåKonkreteElementerIVedtaket = klagesDetPåKonkreteElementerIVedtaket,
    erKlagefristenOverholdt = erKlagefristenOverholdt,
    erUnntakForKlagefrist = when (erUnntakForKlagefrist) {
        null -> null
        KlagefristUnntakSvarord.JA_AV_SÆRLIGE_GRUNNER -> KlagefristUnntakSvarordDto.JA_AV_SÆRLIGE_GRUNNER
        KlagefristUnntakSvarord.JA_KLAGER_KAN_IKKE_LASTES_FOR_Å_HA_SENDT_INN_ETTER_FRISTEN -> KlagefristUnntakSvarordDto.JA_KLAGER_KAN_IKKE_LASTES_FOR_Å_HA_SENDT_INN_ETTER_FRISTEN
        KlagefristUnntakSvarord.NEI -> KlagefristUnntakSvarordDto.NEI
    },
    erKlagenSignert = erKlagenSignert,
    innsendingsdato = innsendingsdato.toString(),
    innsendingskilde = innsendingskilde.toDto(),
)

sealed interface KlagebehandlingsresultatDTO {

    val type: KlageresultatstypeDto
    val brevtekst: List<TittelOgTekstDTO>

    data class Avvist(
        override val brevtekst: List<TittelOgTekstDTO>,
    ) : KlagebehandlingsresultatDTO {
        override val type = KlageresultatstypeDto.AVVIST
    }

    data class Omgjør(
        override val brevtekst: List<TittelOgTekstDTO>,
        val årsak: String,
        val begrunnelse: String,
        val rammebehandlingId: String?,
    ) : KlagebehandlingsresultatDTO {
        override val type = KlageresultatstypeDto.OMGJØR
    }

    data class Opprettholdt(
        override val brevtekst: List<TittelOgTekstDTO>,
        val hjemler: List<KlagehjemmelDto>,
        val iverksattOpprettholdelseTidspunkt: LocalDateTime?,
        val journalføringstidspunktInnstillingsbrev: LocalDateTime?,
        val distribusjonstidspunktInnstillingsbrev: LocalDateTime?,
        val oversendtKlageinstansenTidspunkt: LocalDateTime?,
        val klageinstanshendelser: List<KlageinstanshendelseDTO>,
        val ferdigstiltTidspunkt: LocalDateTime?,
    ) : KlagebehandlingsresultatDTO {
        override val type = KlageresultatstypeDto.OPPRETTHOLDT
    }
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
    klagensJournalpostId = klagensJournalpostId.toString(),
    klagensJournalpostOpprettet = klagensJournalpostOpprettet.toString(),
    status = status.toKlagestatustypeDto(),
    avbrutt = avbrutt?.toAvbruttDTO(),
    kanIverksetteVedtak = kanIverksetteVedtak,
    kanIverksetteOpprettholdelse = kanIverksetteOpprettholdelse,
    ventestatus = ventestatus.ventestatusHendelser.lastOrNull()?.tilVentestatusHendelseDTO(),
    resultat = resultat?.tilKlagebehandlingsresultatDTO(),
    formkrav = formkrav.toDTO(),
)

fun Klagebehandlingsresultat.tilKlagebehandlingsresultatDTO(): KlagebehandlingsresultatDTO {
    val brevtekstDTO = brevtekst?.toDTO() ?: emptyList()

    return when (this) {
        is Klagebehandlingsresultat.Avvist -> KlagebehandlingsresultatDTO.Avvist(
            brevtekst = brevtekstDTO,
        )

        is Klagebehandlingsresultat.Omgjør -> KlagebehandlingsresultatDTO.Omgjør(
            brevtekst = brevtekstDTO,
            årsak = årsak.name,
            begrunnelse = begrunnelse.verdi,
            rammebehandlingId = rammebehandlingId?.toString(),
        )

        is Klagebehandlingsresultat.Opprettholdt -> KlagebehandlingsresultatDTO.Opprettholdt(
            brevtekst = brevtekstDTO,
            hjemler = hjemler.toKlagehjemmelDto(),
            iverksattOpprettholdelseTidspunkt = iverksattOpprettholdelseTidspunkt,
            journalføringstidspunktInnstillingsbrev = journalføringstidspunktInnstillingsbrev,
            distribusjonstidspunktInnstillingsbrev = distribusjonstidspunktInnstillingsbrev,
            oversendtKlageinstansenTidspunkt = oversendtKlageinstansenTidspunkt,
            klageinstanshendelser = klageinstanshendelser.toDTO(),
            ferdigstiltTidspunkt = ferdigstiltTidspunkt,
        )
    }
}
