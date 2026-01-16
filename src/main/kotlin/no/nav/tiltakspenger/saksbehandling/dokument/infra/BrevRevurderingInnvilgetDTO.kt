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

private data class BrevRevurderingInnvilgetDTO(
    override val personalia: BrevPersonaliaDTO,
    override val saksnummer: String,
    override val saksbehandlerNavn: String,
    override val beslutterNavn: String?,
    override val datoForUtsending: String,
    override val tilleggstekst: String?,
    override val forhandsvisning: Boolean,

    override val innvilgelsesperioder: List<BrevPeriodeDTO>,
    override val harBarnetillegg: Boolean,
    override val satser: List<SatserDTO>,
    override val antallDagerTekst: String?,
    override val barnetilleggTekst: String?,

    /**
     *  Intro teksten som vises dersom [harBarnetillegg] er true - ellers vil brevet ha sin egen introtekst uten barnetillegg.
     *  TODO: fjern når pdfgen er oppdatert til å bruke [barnetilleggTekst]
     */
    val introTekstMedBarnetillegg: String?,

    // TODO abn: fraDato og tilDato kan fjernes når pdfgen er oppdatert til å bruke innvilgelsesperioder
    val fraDato: String,
    val tilDato: String,
) : BrevRammevedtakInnvilgelseBaseDTO

suspend fun Rammevedtak.tilRevurderingInnvilgetBrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    vedtaksdato: LocalDate,
    tilleggstekst: FritekstTilVedtaksbrev? = null,
): String {
    return genererRevurderingInnvilgetBrev(
        hentBrukersNavn = hentBrukersNavn,
        hentSaksbehandlersNavn = hentSaksbehandlersNavn,
        vedtaksdato = vedtaksdato,
        tilleggstekst = tilleggstekst,
        fnr = fnr,
        saksbehandlerNavIdent = saksbehandler,
        beslutterNavIdent = beslutter,
        innvilgelsesperioder = innvilgelsesperioder!!.perioder,
        saksnummer = saksnummer,
        forhåndsvisning = false,
        barnetilleggsPerioder = barnetillegg?.periodisering,
        antallDagerTekst = toAntallDagerTekst(antallDagerPerMeldeperiode),
    )
}

internal suspend fun genererRevurderingInnvilgetBrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    tilleggstekst: FritekstTilVedtaksbrev?,
    fnr: Fnr,
    saksbehandlerNavIdent: String,
    beslutterNavIdent: String?,
    saksnummer: Saksnummer,
    innvilgelsesperioder: NonEmptyList<Periode>,
    vedtaksdato: LocalDate,
    barnetilleggsPerioder: Periodisering<AntallBarn>?,
    forhåndsvisning: Boolean,
    antallDagerTekst: String?,
): String {
    val brukersNavn = hentBrukersNavn(fnr)
    val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
    val besluttersNavn = beslutterNavIdent?.let { hentSaksbehandlersNavn(it) }

    val innvilgelseTotalperiode = Periode(
        innvilgelsesperioder.first().fraOgMed,
        innvilgelsesperioder.last().tilOgMed,
    )

    val barnetilleggTekst = barnetilleggsPerioder?.tilIntroTekst(innvilgelseTotalperiode, antallDagerTekst)

    return BrevRevurderingInnvilgetDTO(
        personalia = BrevPersonaliaDTO(
            ident = fnr.verdi,
            fornavn = brukersNavn.fornavn,
            etternavn = brukersNavn.mellomnavnOgEtternavn,
        ),
        saksnummer = saksnummer.verdi,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        innvilgelsesperioder = innvilgelsesperioder.map { BrevPeriodeDTO.fraPeriode(it) },
        fraDato = innvilgelseTotalperiode.fraOgMed.format(norskDatoFormatter),
        tilDato = innvilgelseTotalperiode.tilOgMed.format(norskDatoFormatter),
        satser = Satser.tilSatserDTO(innvilgelseTotalperiode),
        tilleggstekst = tilleggstekst?.verdi,
        forhandsvisning = forhåndsvisning,
        harBarnetillegg = barnetilleggsPerioder != null && barnetilleggsPerioder.any { it.verdi.value > 0 },
        introTekstMedBarnetillegg = barnetilleggTekst,
        barnetilleggTekst = barnetilleggTekst,
        datoForUtsending = vedtaksdato.format(norskDatoFormatter),
        antallDagerTekst = antallDagerTekst,
    ).let { serialize(it) }
}

private fun Periodisering<AntallBarn>.tilIntroTekst(vedtaksperiode: Periode, antallDagerTekst: String?): String? {
    val antallDagerPerUkeTekst = antallDagerTekst?.let { " for $it" } ?: ""
    val perioderMedBarnetillegg = perioderMedVerdi
        .filter { it.verdi.value > 0 }

    if (perioderMedBarnetillegg.isEmpty()) {
        return null
    }

    val harBarnetilleggOverHeleInnvilgelsesperiode = perioder.all { periode -> vedtaksperiode == periode }

    val perioderMedBarnetilleggString = perioderMedBarnetillegg
        .map { periodeMedVerdi ->
            val antallBarn = periodeMedVerdi.verdi.toTekst()
            "for $antallBarn barn fra og med ${periodeMedVerdi.periode.fraOgMed.format(norskDatoFormatter)} til og med ${
                periodeMedVerdi.periode.tilOgMed.format(norskDatoFormatter)
            }"
        }.let {
            when (it.size) {
                0 -> throw IllegalStateException("Skal ikke være mulig å ha 0 perioder med barnetillegg her!")
                1 -> it.first()
                2 -> "${it.first()} og ${it.last()}"
                else -> it.dropLast(1).joinToString(", ").plus(" og ${it.last()}")
            }
        }

    return if (harBarnetilleggOverHeleInnvilgelsesperiode) {
        "Du får tiltakspenger og barnetillegg $perioderMedBarnetilleggString$antallDagerPerUkeTekst."
    } else {
        """
            Du får tiltakspenger fra og med ${vedtaksperiode.fraOgMed.format(norskDatoFormatter)} til og med ${
            vedtaksperiode.tilOgMed.format(norskDatoFormatter)
        }$antallDagerPerUkeTekst.
        
            Du får barnetillegg $perioderMedBarnetilleggString.
        """.trimIndent()
    }
}
