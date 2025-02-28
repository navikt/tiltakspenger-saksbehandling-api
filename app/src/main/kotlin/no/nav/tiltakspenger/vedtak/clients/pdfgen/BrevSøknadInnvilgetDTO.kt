package no.nav.tiltakspenger.vedtak.clients.pdfgen

import no.nav.tiltakspenger.barnetillegg.AntallBarn
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.behandling.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.domene.personopplysninger.Navn
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.utbetaling.domene.Satser
import no.nav.tiltakspenger.vedtak.clients.pdfgen.formattering.norskDatoFormatter
import java.time.LocalDate

@Suppress("unused")
private data class BrevFørstegangsvedtakInnvilgelseDTO(
    val personalia: BrevPersonaliaDTO,
    val tiltaksnavn: String,
    val rammevedtakFraDato: String,
    val rammevedtakTilDato: String,
    val saksnummer: String,
    val antallBarn: List<AntallBarnPerPeriodeDTO>,
    val antallBarnHvis1PeriodeIHeleInnvilgelsesperiode: Int?,
    val saksbehandlerNavn: String,
    val beslutterNavn: String?,
    val kontor: String,
    val datoForUtsending: String,
    val sats: Int,
    val satsBarn: Int,
    val tilleggstekst: String? = null,
) {
    val barnetillegg: Boolean = antallBarn.isNotEmpty()

    data class AntallBarnPerPeriodeDTO(
        val antallBarn: Int,
        val fraOgMed: String,
        val tilOgMed: String,
    )
}

internal suspend fun Rammevedtak.toInnvilgetSøknadsbrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    vedtaksdato: LocalDate,
    tilleggstekst: FritekstTilVedtaksbrev? = null,
): String {
    return genererInnvilgetSøknadsbrev(
        hentBrukersNavn = hentBrukersNavn,
        hentSaksbehandlersNavn = hentSaksbehandlersNavn,
        vedtaksdato = vedtaksdato,
        tilleggstekst = tilleggstekst,
        fnr = fnr,
        saksbehandlerNavIdent = saksbehandlerNavIdent,
        beslutterNavIdent = beslutterNavIdent,
        tiltaksnavn = this.behandling.tiltaksnavn,
        innvilgelsesperiode = this.periode,
        saksnummer = saksnummer,
        barnetilleggsPerioder = this.behandling.barnetillegg?.periodisering,
    )
}

internal suspend fun genererInnvilgetSøknadsbrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    vedtaksdato: LocalDate,
    tilleggstekst: FritekstTilVedtaksbrev? = null,
    fnr: Fnr,
    saksbehandlerNavIdent: String,
    beslutterNavIdent: String?,
    tiltaksnavn: String,
    innvilgelsesperiode: Periode,
    saksnummer: Saksnummer,
    barnetilleggsPerioder: Periodisering<AntallBarn>?,
): String {
    val brukersNavn = hentBrukersNavn(fnr)
    val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
    val besluttersNavn = beslutterNavIdent?.let { hentSaksbehandlersNavn(it) }

    return BrevFørstegangsvedtakInnvilgelseDTO(
        personalia = BrevPersonaliaDTO(
            ident = fnr.verdi,
            fornavn = brukersNavn.fornavn,
            etternavn = brukersNavn.mellomnavnOgEtternavn,
            antallBarn = 0,
        ),
        tiltaksnavn = tiltaksnavn,
        rammevedtakFraDato = innvilgelsesperiode.fraOgMed.format(norskDatoFormatter),
        rammevedtakTilDato = innvilgelsesperiode.tilOgMed.format(norskDatoFormatter),
        saksnummer = saksnummer.verdi,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        // TODO post-mvp: legg inn NORG integrasjon for å hente saksbehandlers kontor.
        kontor = "Nav Tiltak Øst-Viken",
        // Dette er vår dato, det brukes typisk når bruker klager på vedtaksbrev på dato ...
        datoForUtsending = vedtaksdato.format(norskDatoFormatter),
        sats = Satser.sats(innvilgelsesperiode.fraOgMed).sats,
        satsBarn = Satser.sats(innvilgelsesperiode.fraOgMed).satsBarnetillegg,
        tilleggstekst = tilleggstekst?.verdi,
        antallBarn = barnetilleggsPerioder?.perioderMedVerdi?.map {
            BrevFørstegangsvedtakInnvilgelseDTO.AntallBarnPerPeriodeDTO(
                antallBarn = it.verdi.value,
                fraOgMed = it.periode.fraOgMed.format(norskDatoFormatter),
                tilOgMed = it.periode.tilOgMed.format(norskDatoFormatter),
            )
        } ?: emptyList(),
        antallBarnHvis1PeriodeIHeleInnvilgelsesperiode = when {
            barnetilleggsPerioder?.size != 1 -> null
            barnetilleggsPerioder.first().periode == innvilgelsesperiode -> barnetilleggsPerioder.first().verdi.value
            else -> null
        },
    ).let { serialize(it) }
}
