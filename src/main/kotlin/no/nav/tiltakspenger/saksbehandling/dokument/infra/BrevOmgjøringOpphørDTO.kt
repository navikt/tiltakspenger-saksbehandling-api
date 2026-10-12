package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

private data class BrevOmgjøringOpphørDTO(
    override val personalia: BrevPersonaliaDTO,
    override val saksnummer: String,
    override val saksbehandlerNavn: String,
    override val beslutterNavn: String?,
    override val datoForUtsending: String,
    override val tilleggstekst: String?,
    override val forhandsvisning: Boolean,
    val vedtaksperiode: BrevPeriodeDTO,
    val opphørsgrunner: List<GrunnBrevDto>,
    val barnetillegg: Boolean,
) : BrevRammevedtakBaseDTO

suspend fun Rammevedtak.tilBrevOmgjøringOpphørDTO(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    harOpphørtBarnetillegg: Boolean,
    vedtaksdato: LocalDate,
): String {
    require(rammebehandling is Revurdering && rammebehandling.resultat is Omgjøringsresultat.OmgjøringOpphør)

    return genererOpphørBrev(
        hentBrukersNavn = hentBrukersNavn,
        hentSaksbehandlersNavn = hentSaksbehandlersNavn,
        vedtaksdato = vedtaksdato,
        fnr = fnr,
        saksbehandlerNavIdent = saksbehandler,
        beslutterNavIdent = beslutter,
        saksnummer = saksnummer,
        forhåndsvisning = false,
        vedtaksperiode = this.periode,
        valgteHjemler = rammebehandling.resultat.valgteHjemler,
        tilleggstekst = rammebehandling.fritekstTilVedtaksbrev,
        harOpphørtBarnetillegg = harOpphørtBarnetillegg,
    )
}

suspend fun genererOpphørBrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    harOpphørtBarnetillegg: Boolean,
    vedtaksdato: LocalDate,
    fnr: Fnr,
    saksbehandlerNavIdent: String,
    beslutterNavIdent: String?,
    saksnummer: Saksnummer,
    forhåndsvisning: Boolean,
    vedtaksperiode: Periode,
    valgteHjemler: NonEmptySet<HjemmelForOpphør>,
    tilleggstekst: FritekstTilVedtaksbrev? = null,
): String {
    val brukersNavn = hentBrukersNavn(fnr)
    val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
    val besluttersNavn = beslutterNavIdent?.let { hentSaksbehandlersNavn(it) }

    require(valgteHjemler.any { it !in Omgjøringsresultat.OmgjøringOpphør.hjemlerSomMåHaFritekst } || tilleggstekst != null) {
        "For opphørsbrev må det være enten valgte hjemler med tekst eller en tilleggstekst - hjemler: $valgteHjemler"
    }

    return BrevOmgjøringOpphørDTO(
        personalia = BrevPersonaliaDTO(
            ident = fnr.verdi,
            fornavn = brukersNavn.fornavn,
            etternavn = brukersNavn.mellomnavnOgEtternavn,
        ),
        vedtaksperiode = BrevPeriodeDTO.fraPeriode(vedtaksperiode),
        saksnummer = saksnummer.verdi,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        // Dette er vår dato, det brukes typisk når bruker klager på vedtaksbrev på dato ...
        datoForUtsending = vedtaksdato.format(norskDatoFormatter),
        forhandsvisning = forhåndsvisning,
        opphørsgrunner = valgteHjemler.map { it.tilGrunnBrevDto() },
        tilleggstekst = tilleggstekst?.verdi,
        barnetillegg = harOpphørtBarnetillegg,
    ).let { serialize(it) }
}
