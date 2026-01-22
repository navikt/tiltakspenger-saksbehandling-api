package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import no.nav.tiltakspenger.saksbehandling.infra.route.AvbruttDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.toAvbruttDTO
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse

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
    val status: String,
    val resultat: String?,
    val vedtakDetKlagesPå: String?,
    val erKlagerPartISaken: Boolean,
    val klagesDetPåKonkreteElementerIVedtaket: Boolean,
    val erKlagefristenOverholdt: Boolean,
    val erUnntakForKlagefrist: KlagefristUnntakSvarordDto?,
    val erKlagenSignert: Boolean,
    val brevtekst: List<TittelOgTekstDTO>,
    val avbrutt: AvbruttDTO?,
    val kanIverksette: Boolean,
    val årsak: String?,
    val begrunnelse: String?,
) {
    data class TittelOgTekstDTO(
        val tittel: String,
        val tekst: String,
    )
}

fun Klagebehandling.toDto() = KlagebehandlingDTO(
    id = id.toString(),
    sakId = sakId.toString(),
    saksnummer = saksnummer.toString(),
    fnr = fnr.verdi,
    opprettet = opprettet.toString(),
    sistEndret = sistEndret.toString(),
    iverksattTidspunkt = iverksattTidspunkt?.toString(),
    saksbehandler = saksbehandler,
    journalpostId = journalpostId.toString(),
    journalpostOpprettet = journalpostOpprettet.toString(),
    status = when (status) {
        Klagebehandlingsstatus.KLAR_TIL_BEHANDLING -> "KLAR_TIL_BEHANDLING"
        Klagebehandlingsstatus.UNDER_BEHANDLING -> "UNDER_BEHANDLING"
        Klagebehandlingsstatus.AVBRUTT -> "AVBRUTT"
        Klagebehandlingsstatus.IVERKSATT -> "IVERKSATT"
    },
    resultat = when (resultat) {
        null -> null
        is Klagebehandlingsresultat.Avvist -> "AVVIST"
        is Klagebehandlingsresultat.Omgjør -> "OMGJØR"
    },
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
    kanIverksette = kanIverksette,
    årsak = (resultat as? Klagebehandlingsresultat.Omgjør)?.årsak?.toString(),
    begrunnelse = (resultat as? Klagebehandlingsresultat.Omgjør)?.begrunnelse?.verdi,
)
