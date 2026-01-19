package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.leggSammen
import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.libs.periodisering.perioder
import no.nav.tiltakspenger.libs.satser.Satser
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
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

    override val introTekst: String,
    override val harBarnetillegg: Boolean,
    override val satser: List<SatserDTO>,
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
        saksnummer = saksnummer,
        forhåndsvisning = false,
        innvilgelsesperioder = innvilgelsesperioder!!,
        barnetillegg = barnetillegg?.periodisering,
    )
}

suspend fun genererRevurderingInnvilgetBrev(
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

    return BrevRevurderingInnvilgetDTO(
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
        introTekst = tilIntroTekst(innvilgelsesperioder, barnetillegg),
        datoForUtsending = vedtaksdato.format(norskDatoFormatter),
    ).let { serialize(it) }
}

private fun tilIntroTekst(
    innvilgelsesperioder: Innvilgelsesperioder,
    barnetillegg: Periodisering<AntallBarn>?,
): String {
    val antallDagerPerUkeTekst =
        toAntallDagerTekst(innvilgelsesperioder.antallDagerPerMeldeperiode)?.let { " for $it" } ?: ""

    val perioderMedBarnetillegg = barnetillegg?.perioderMedVerdi?.filter { it.verdi.value > 0 }

    val sammenhengendeInnvilgelsesperioder = innvilgelsesperioder.perioder.leggSammen(false)

    val harBarnetilleggForHeleInnvilgelsesperioden =
        perioderMedBarnetillegg?.perioder() == sammenhengendeInnvilgelsesperioder

    val perioderMedBarnetilleggString = perioderMedBarnetillegg
        ?.map { periodeMedVerdi ->
            val antallBarn = periodeMedVerdi.verdi.toTekst()
            "for $antallBarn barn fra og med ${periodeMedVerdi.periode.fraOgMed.format(norskDatoFormatter)} til og med ${
                periodeMedVerdi.periode.tilOgMed.format(norskDatoFormatter)
            }"
        }?.joinMedKonjunksjon()

    return if (harBarnetilleggForHeleInnvilgelsesperioden) {
        "Du får tiltakspenger og barnetillegg $perioderMedBarnetilleggString$antallDagerPerUkeTekst."
    } else {
        val tiltakspengerString = sammenhengendeInnvilgelsesperioder.map {
            "fra og med ${it.fraOgMed.format(norskDatoFormatter)} til og med ${it.tilOgMed.format(norskDatoFormatter)}"
        }.joinMedKonjunksjon()

        "Du får tiltakspenger $tiltakspengerString$antallDagerPerUkeTekst.".plus(
            perioderMedBarnetilleggString?.let { "\n\nDu får barnetillegg $perioderMedBarnetilleggString." } ?: "",
        )
    }
}
