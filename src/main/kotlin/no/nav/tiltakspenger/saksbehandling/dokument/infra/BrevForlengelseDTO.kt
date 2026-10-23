package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.satser.Satser
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.dokument.infra.BrevRammevedtakInnvilgelseBaseDTO.SatserDTO
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

/**
 * Vedtaksbrevet for en forlengelse.
 * Har de samme feltene som brevet for revurdering til innvilgelse, i tillegg til perioden som forlenges.
 *
 * @param forlengelsesperiode Fra dagen etter forrige vedtak, til og med ny til og med-dato.
 */
private data class BrevForlengelseDTO(
    override val personalia: BrevPersonaliaDTO,
    override val saksnummer: String,
    override val saksbehandlerNavn: String,
    override val beslutterNavn: String?,
    override val datoForUtsending: String,
    override val tilleggstekst: String?,
    override val forhandsvisning: Boolean,
    override val harBarnetillegg: Boolean,
    override val satser: List<SatserDTO>,
    override val innvilgelsesperioder: BrevInnvilgelsesperioderDTO,
    override val barnetillegg: List<BrevBarnetilleggDTO>,
    val forlengelsesperiode: BrevPeriodeDTO,
) : BrevRammevedtakInnvilgelseBaseDTO

suspend fun Rammevedtak.tilForlengelseBrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    vedtaksdato: LocalDate,
    tilleggstekst: FritekstTilVedtaksbrev?,
): String {
    return genererForlengelseBrev(
        hentBrukersNavn = hentBrukersNavn,
        hentSaksbehandlersNavn = hentSaksbehandlersNavn,
        vedtaksdato = vedtaksdato,
        tilleggstekst = tilleggstekst,
        fnr = fnr,
        saksbehandlerNavIdent = saksbehandler,
        beslutterNavIdent = beslutter,
        saksnummer = saksnummer,
        forhåndsvisning = false,
        innvilgelsesperioder = innvilgelsesperioder!!,
        barnetillegg = barnetillegg?.periodisering,
    )
}

suspend fun genererForlengelseBrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    fnr: Fnr,
    saksbehandlerNavIdent: String,
    beslutterNavIdent: String?,
    saksnummer: Saksnummer,
    vedtaksdato: LocalDate,
    forhåndsvisning: Boolean,
    innvilgelsesperioder: Innvilgelsesperioder,
    barnetillegg: Periodisering<AntallBarn>?,
    tilleggstekst: FritekstTilVedtaksbrev?,
): String {
    val brukersNavn = hentBrukersNavn(fnr)
    val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
    val besluttersNavn = beslutterNavIdent?.let { hentSaksbehandlersNavn(it) }

    return BrevForlengelseDTO(
        personalia = BrevPersonaliaDTO(
            ident = fnr.verdi,
            fornavn = brukersNavn.fornavn,
            etternavn = brukersNavn.mellomnavnOgEtternavn,
        ),
        saksnummer = saksnummer.verdi,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        satser = Satser.tilSatserDTO(innvilgelsesperioder.totalPeriode),
        tilleggstekst = tilleggstekst?.verdi,
        forhandsvisning = forhåndsvisning,
        harBarnetillegg = barnetillegg != null && barnetillegg.any { it.verdi.value > 0 },
        datoForUtsending = vedtaksdato.format(norskDatoFormatter),
        innvilgelsesperioder = tilBrevInnvilgelsesperioder(innvilgelsesperioder),
        barnetillegg = tilBrevBarnetillegg(barnetillegg),
        forlengelsesperiode = BrevPeriodeDTO.fraPeriode(innvilgelsesperioder.totalPeriode),
    ).let { serialize(it) }
}
