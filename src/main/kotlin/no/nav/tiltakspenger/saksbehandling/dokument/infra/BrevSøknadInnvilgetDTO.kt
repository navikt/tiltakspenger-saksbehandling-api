package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Satser
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

@Suppress("unused")
private data class BrevFørstegangsvedtakInnvilgelseDTO(
    val personalia: BrevPersonaliaDTO,
    val rammevedtakFraDato: String,
    val rammevedtakTilDato: String,
    val saksnummer: String,
    val antallBarn: List<AntallBarnPerPeriodeDTO>,
    val barnetilleggTekst: String?,
    val antallBarnHvis1PeriodeIHeleInnvilgelsesperiode: Int?,
    val saksbehandlerNavn: String,
    val beslutterNavn: String?,
    val kontor: String,
    val datoForUtsending: String,
    val satser: List<Any>,
    val satsBarn: Int,
    val tilleggstekst: String? = null,
    val forhandsvisning: Boolean,
) {
    val barnetillegg: Boolean = antallBarn.isNotEmpty()

    data class AntallBarnPerPeriodeDTO(
        val antallBarn: Int,
        val antallBarnTekst: String,
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
        innvilgelsesperiode = this.periode,
        saksnummer = saksnummer,
        // finnes ikke noe forhåndsvisning for rammevedtak
        forhåndsvisning = false,
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
    innvilgelsesperiode: Periode,
    saksnummer: Saksnummer,
    forhåndsvisning: Boolean,
    barnetilleggsPerioder: Periodisering<AntallBarn>?,
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
        kontor = "Nav Tiltakspenger",
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
    ).let { serialize(it) }
}
