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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse.BarnSomFyller16
import no.nav.tiltakspenger.saksbehandling.dokument.infra.BrevRammevedtakInnvilgelseBaseDTO.SatserDTO
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

/**
 * Vedtaksbrevet for en forlengelse.
 * Har de samme feltene som brevet for revurdering til innvilgelse, i tillegg til barna som fyller 16 år i forlengelsen.
 *
 * @param barnSomFyller16 Null når ingen barn fyller 16 år i forlengelsen, eller saksbehandler har endret barnetillegget.
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
    val barnSomFyller16: BarnSomFyller16DTO?,
) : BrevRammevedtakInnvilgelseBaseDTO

/**
 * Se [BarnSomFyller16].
 */
private data class BarnSomFyller16DTO(
    val antall: Int,
    val antallBarnIVedtaket: Int,
    val sisteDagMedBarnetillegg: String,
)

suspend fun Rammevedtak.tilForlengelseBrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    vedtaksdato: LocalDate,
    tilleggstekst: FritekstTilVedtaksbrev?,
    barnSomFyller16: BarnSomFyller16?,
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
        barnSomFyller16 = barnSomFyller16,
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
    barnSomFyller16: BarnSomFyller16?,
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
        barnSomFyller16 = barnSomFyller16?.let {
            BarnSomFyller16DTO(
                antall = it.antall,
                antallBarnIVedtaket = it.antallBarnIVedtaket,
                sisteDagMedBarnetillegg = it.sisteDagMedBarnetillegg.format(norskDatoFormatter),
            )
        },
    ).let { serialize(it) }
}
