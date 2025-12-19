package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.libs.satser.Satser
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
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
    val rammevedtakFraDato: String,
    val rammevedtakTilDato: String,
    val antallBarn: List<AntallBarnPerPeriodeDTO>,
    val barnetilleggTekst: String?,
    val antallBarnHvis1PeriodeIHeleInnvilgelsesperiode: Int?,
    val satser: List<Any>,
    val satsBarn: Int,
    val antallDagerTekst: String?,
) : BrevRammevedtakBaseDTO {

    @Suppress("unused")
    val barnetillegg: Boolean = antallBarn.isNotEmpty()

    data class AntallBarnPerPeriodeDTO(
        val antallBarn: Int,
        val antallBarnTekst: String,
        val fraOgMed: String,
        val tilOgMed: String,
    )
}

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
        innvilgelsesperiode = this.periode,
        saksnummer = saksnummer,
        forhåndsvisning = false,
        barnetilleggsPerioder = this.behandling.barnetillegg?.periodisering,
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
    innvilgelsesperiode: Periode,
    saksnummer: Saksnummer,
    forhåndsvisning: Boolean,
    barnetilleggsPerioder: Periodisering<AntallBarn>?,
    antallDagerTekst: String?,
): String {
    val brukersNavn = hentBrukersNavn(fnr)
    val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
    val besluttersNavn = beslutterNavIdent?.let { hentSaksbehandlersNavn(it) }

    val antallBarn = barnetilleggsPerioder?.perioderMedVerdi?.filter {
        it.verdi.value > 0
    }?.map {
        BrevFørstegangsvedtakInnvilgelseDTO.AntallBarnPerPeriodeDTO(
            antallBarn = it.verdi.value,
            antallBarnTekst = it.verdi.toTekst(),
            fraOgMed = it.periode.fraOgMed.format(norskDatoFormatter),
            tilOgMed = it.periode.tilOgMed.format(norskDatoFormatter),
        )
    } ?: emptyList()

    val barnetilleggTekst = if (antallBarn.isNotEmpty()) {
        "Du får barnetillegg for ${
            antallBarn.joinToString(" og ") {
                "${it.antallBarnTekst} barn fra ${it.fraOgMed} til og med ${it.tilOgMed}"
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
        rammevedtakFraDato = innvilgelsesperiode.fraOgMed.format(norskDatoFormatter),
        rammevedtakTilDato = innvilgelsesperiode.tilOgMed.format(norskDatoFormatter),
        saksnummer = saksnummer.verdi,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        // Dette er vår dato, det brukes typisk når bruker klager på vedtaksbrev på dato ...
        datoForUtsending = vedtaksdato.format(norskDatoFormatter),
        satser = Satser.satser.filter { it.periode.overlapperMed(innvilgelsesperiode) }.map {
            @Suppress("unused")
            object {
                val år = it.periode.fraOgMed.year
                val ordinær = it.sats
                val barnetillegg = it.satsBarnetillegg
            }
        },
        satsBarn = Satser.sats(innvilgelsesperiode.fraOgMed).satsBarnetillegg,
        tilleggstekst = tilleggstekst?.verdi,
        forhandsvisning = forhåndsvisning,
        antallBarn = antallBarn,
        barnetilleggTekst = barnetilleggTekst,
        antallBarnHvis1PeriodeIHeleInnvilgelsesperiode = when {
            barnetilleggsPerioder?.size != 1 -> null
            barnetilleggsPerioder.first().periode == innvilgelsesperiode -> barnetilleggsPerioder.first().verdi.value
            else -> null
        },
        antallDagerTekst = antallDagerTekst,
    ).let { serialize(it) }
}
