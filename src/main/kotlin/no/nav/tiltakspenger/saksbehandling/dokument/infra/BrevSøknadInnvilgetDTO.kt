package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.libs.satser.Satser
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.dokument.infra.BrevRammevedtakInnvilgelseBaseDTO.SatserDTO
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

private data class BrevFørstegangsvedtakInnvilgelseDTO(
    override val personalia: BrevPersonaliaDTO,
    override val saksnummer: String,
    override val saksbehandlerNavn: String,
    override val beslutterNavn: String?,
    override val datoForUtsending: String,
    override val tilleggstekst: String?,
    override val forhandsvisning: Boolean,

    override val innvilgelsesperioder: List<BrevPeriodeDTO>,
    override val harBarnetillegg: Boolean,
    override val barnetilleggTekst: String?,
    override val satser: List<SatserDTO>,
    override val antallDagerTekst: String?,

    // TODO abn: rammevedtakFraDato og rammevedtakTilDato kan fjernes når pdfgen er oppdatert til å bruke innvilgelsesperioder
    val rammevedtakFraDato: String,
    val rammevedtakTilDato: String,
) : BrevRammevedtakInnvilgelseBaseDTO

internal suspend fun Rammevedtak.tilInnvilgetSøknadsbrev(
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
        saksbehandlerNavIdent = saksbehandler,
        beslutterNavIdent = beslutter,
        innvilgelsesperioder = this.innvilgelsesperioder!!.perioder,
        saksnummer = saksnummer,
        forhåndsvisning = false,
        barnetilleggsPerioder = this.behandling.barnetillegg!!.periodisering,
        antallDagerTekst = toAntallDagerTekst(this.antallDagerPerMeldeperiode),
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
    innvilgelsesperioder: NonEmptyList<Periode>,
    saksnummer: Saksnummer,
    forhåndsvisning: Boolean,
    barnetilleggsPerioder: Periodisering<AntallBarn>?,
    antallDagerTekst: String?,
): String {
    val brukersNavn = hentBrukersNavn(fnr)
    val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
    val besluttersNavn = beslutterNavIdent?.let { hentSaksbehandlersNavn(it) }

    val innvilgelseTotalperiode = Periode(
        innvilgelsesperioder.first().fraOgMed,
        innvilgelsesperioder.last().tilOgMed,
    )

    val perioderMedBarn = barnetilleggsPerioder?.perioderMedVerdi?.filter {
        it.verdi.value > 0
    } ?: emptyList()

    val barnetilleggTekst = if (perioderMedBarn.isNotEmpty()) {
        "Du får barnetillegg for ${
            perioderMedBarn.joinToString(" og ") {
                "${it.verdi.toTekst()} barn fra ${it.periode.fraOgMed.format(norskDatoFormatter)} til og med ${
                    it.periode.tilOgMed.format(
                        norskDatoFormatter,
                    )
                }"
            }
        }."
    } else {
        null
    }

    return BrevFørstegangsvedtakInnvilgelseDTO(
        personalia = BrevPersonaliaDTO(
            ident = fnr.verdi,
            fornavn = brukersNavn.fornavn,
            etternavn = brukersNavn.mellomnavnOgEtternavn,
        ),
        rammevedtakFraDato = innvilgelseTotalperiode.fraOgMed.format(norskDatoFormatter),
        rammevedtakTilDato = innvilgelseTotalperiode.tilOgMed.format(norskDatoFormatter),
        innvilgelsesperioder = innvilgelsesperioder.map { BrevPeriodeDTO.fraPeriode(it) },
        saksnummer = saksnummer.verdi,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        datoForUtsending = vedtaksdato.format(norskDatoFormatter),
        satser = Satser.tilSatserDTO(innvilgelseTotalperiode),
        tilleggstekst = tilleggstekst?.verdi,
        forhandsvisning = forhåndsvisning,
        harBarnetillegg = perioderMedBarn.isNotEmpty(),
        barnetilleggTekst = barnetilleggTekst,
        antallDagerTekst = antallDagerTekst,
    ).let { serialize(it) }
}
