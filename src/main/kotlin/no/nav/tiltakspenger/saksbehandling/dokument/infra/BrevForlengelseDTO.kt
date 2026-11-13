package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.satser.Satser
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse.BarnSomFyller16
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

/**
 * Vedtaksbrevet for en forlengelse.
 * Har de samme feltene som brevet for revurdering til innvilgelse, i tillegg til barna som fyller 16 år i forlengelsen.
 * [harBarnetillegg] er bare med i dette brevet, der innledningen sier «tiltakspenger og barnetillegg».
 *
 * @param barnSomFyller16 Null når ingen barn fyller 16 år i forlengelsen, eller saksbehandler har endret barnetillegget.
 */
private data class BrevForlengelseDTO(
    override val personalia: BrevPersonaliaDTO,
    override val saksnummer: String,
    override val saksbehandlerNavn: String,
    override val beslutterNavn: String?,
    override val datoForUtsending: LocalDate,
    override val tilleggstekst: String?,
    override val forhandsvisning: Boolean,
    override val perioder: List<PeriodeInnvilgelseDTO>,
    override val sisteKjenteSatsår: Int,
    override val innvilgelsesperioder: BrevInnvilgelsesperioderDTO,
    val harBarnetillegg: Boolean,
    val barnSomFyller16: BarnSomFyller16DTO?,
) : BrevRammevedtakInnvilgelseBaseDTO

/**
 * Se [BarnSomFyller16].
 */
private data class BarnSomFyller16DTO(
    val antall: Int,
    val antallBarnIVedtaket: Int,
    val sisteDagMedBarnetillegg: LocalDate,
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
        barnetillegg = barnetilleggsperioder,
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
        perioder = totalDagSatsForAllePeriodene(innvilgelsesperioder, barnetillegg),
        sisteKjenteSatsår = Satser.sisteKjenteSatsår(),
        tilleggstekst = tilleggstekst?.verdi,
        forhandsvisning = forhåndsvisning,
        harBarnetillegg = barnetillegg != null && barnetillegg.any { it.verdi.value > 0 },
        datoForUtsending = vedtaksdato,
        innvilgelsesperioder = innvilgelsesperioder.tilBrevInnvilgelsesperioder(),
        barnSomFyller16 = barnSomFyller16?.let {
            BarnSomFyller16DTO(
                antall = it.antall,
                antallBarnIVedtaket = it.antallBarnIVedtaket,
                sisteDagMedBarnetillegg = it.sisteDagMedBarnetillegg,
            )
        },
    ).let { serialize(it) }
}
