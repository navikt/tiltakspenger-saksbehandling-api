package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

private data class BrevRevurderingInnvilgetDTO(
    val personalia: BrevPersonaliaDTO,
    val saksnummer: String,
    val saksbehandlerNavn: String,
    val beslutterNavn: String?,
    val kontor: String,
    val fraDato: String,
    val tilDato: String,
    val barnetilleggTekst: String?,
    val tiltakspengesats: Int,
    val barnetilleggsats: Int?,
    val saksbehandlerVurdering: String?,
    val hjemlerTekst: String,
    val forhåndsvisning: Boolean,
)

// TODO raq - må lage noen tester for denne etterhvert som vi får inn mer viktig logikk
internal suspend fun genererRevurderingInnvilgetBrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    saksbehandlersVurdering: FritekstTilVedtaksbrev = FritekstTilVedtaksbrev(""),
    fnr: Fnr,
    saksbehandlerNavIdent: String,
    beslutterNavIdent: String?,
    saksnummer: Saksnummer,
    forhåndsvisning: Boolean,
    vurderingsperiode: Periode,
): String {
    val brukersNavn = hentBrukersNavn(fnr)
    val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
    val besluttersNavn = beslutterNavIdent?.let { hentSaksbehandlersNavn(it) }
    // TODO raq - default. Denne må vi hente fra et annet sted
    val tiltakspengesats = 298
    // TODO raq - default. Denne må vi hente fra et annet sted
    val barnetilleggsats = 55
    val barnetillegTekst = null
    val hjemmelTekst = "Som følge av §§ 1, 2 første ledd og 3 i forskrift om tiltakspenger under tiltakspenger"

    return BrevRevurderingInnvilgetDTO(
        personalia = BrevPersonaliaDTO(
            ident = fnr.verdi,
            fornavn = brukersNavn.fornavn,
            etternavn = brukersNavn.mellomnavnOgEtternavn,
        ),
        saksnummer = saksnummer.verdi,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        // TODO raq - skal denne også gjøres?
        // TODO post-mvp: legg inn NORG integrasjon for å hente saksbehandlers kontor.
        kontor = "Nav Tiltakspenger",
        fraDato = vurderingsperiode.fraOgMed.format(norskDatoFormatter),
        tilDato = vurderingsperiode.tilOgMed.format(norskDatoFormatter),
        barnetilleggTekst = barnetillegTekst,
        tiltakspengesats = tiltakspengesats,
        barnetilleggsats = barnetilleggsats,
        saksbehandlerVurdering = saksbehandlersVurdering.verdi,
        hjemlerTekst = hjemmelTekst,
        forhåndsvisning = forhåndsvisning,
    ).let { serialize(it) }
}
