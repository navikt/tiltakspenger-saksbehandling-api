package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

private data class BrevRevurderingStansDTO(
    override val personalia: BrevPersonaliaDTO,
    override val saksnummer: String,
    override val saksbehandlerNavn: String,
    override val beslutterNavn: String?,
    override val datoForUtsending: String,
    override val tilleggstekst: String?,
    override val forhandsvisning: Boolean,
    val stansFraOgMedDato: String,
    val stansgrunner: List<GrunnBrevDto>,
    val barnetillegg: Boolean,
) : BrevRammevedtakBaseDTO

suspend fun Rammevedtak.toRevurderingStans(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    vedtaksdato: LocalDate,
    harStansetBarnetillegg: Boolean,
): String {
    require(rammebehandling is Revurdering && rammebehandling.resultat is Revurderingsresultat.Stans)

    return genererStansbrev(
        hentBrukersNavn = hentBrukersNavn,
        hentSaksbehandlersNavn = hentSaksbehandlersNavn,
        vedtaksdato = vedtaksdato,
        fnr = fnr,
        saksbehandlerNavIdent = saksbehandler,
        beslutterNavIdent = beslutter,
        stansperiode = this.periode,
        saksnummer = saksnummer,
        forhåndsvisning = false,
        valgteHjemler = rammebehandling.resultat.valgtHjemmel!!,
        tilleggstekst = rammebehandling.fritekstTilVedtaksbrev,
        harStansetBarnetillegg = harStansetBarnetillegg,
    )
}

suspend fun genererStansbrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    vedtaksdato: LocalDate,
    fnr: Fnr,
    saksbehandlerNavIdent: String,
    beslutterNavIdent: String?,
    stansperiode: Periode,
    saksnummer: Saksnummer,
    forhåndsvisning: Boolean,
    valgteHjemler: NonEmptySet<HjemmelForStans>,
    tilleggstekst: FritekstTilVedtaksbrev? = null,
    harStansetBarnetillegg: Boolean,
): String {
    val brukersNavn = hentBrukersNavn(fnr)
    val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
    val besluttersNavn = beslutterNavIdent?.let { hentSaksbehandlersNavn(it) }

    return BrevRevurderingStansDTO(
        personalia = BrevPersonaliaDTO(
            ident = fnr.verdi,
            fornavn = brukersNavn.fornavn,
            etternavn = brukersNavn.mellomnavnOgEtternavn,
        ),
        stansFraOgMedDato = stansperiode.fraOgMed.format(norskDatoFormatter),
        saksnummer = saksnummer.verdi,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        // Dette er vår dato, det brukes typisk når bruker klager på vedtaksbrev på dato ...
        datoForUtsending = vedtaksdato.format(norskDatoFormatter),
        forhandsvisning = forhåndsvisning,
        stansgrunner = valgteHjemler.map { it.tilGrunnBrevDto() },
        tilleggstekst = tilleggstekst?.verdi,
        barnetillegg = harStansetBarnetillegg,
    ).let { serialize(it) }
}
