package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.leggSammen
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.satser.Satser
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
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
    @Deprecated("Erstattes av innvilgelsesperioder og barnetillegg - så kan pdfgen få bestemme hvordan de skal presenteres. Datoene blir formatert for å slippe å gjøre det i pdfgen")
    override val introTekst: String,
    override val harBarnetillegg: Boolean,
    override val satser: List<SatserDTO>,
    override val innvilgelsesperioder: List<BrevPeriodeDTO>,
    override val barnetillegg: List<BrevBarnetilleggDTO>,
) : BrevRammevedtakInnvilgelseBaseDTO

suspend fun Rammevedtak.tilInnvilgetSøknadsbrev(
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
        saksnummer = saksnummer,
        forhåndsvisning = false,
        innvilgelsesperioder = this.rammebehandling.innvilgelsesperioder!!,
        barnetillegg = this.rammebehandling.barnetillegg!!.periodisering,
    )
}

suspend fun genererInnvilgetSøknadsbrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    vedtaksdato: LocalDate,
    tilleggstekst: FritekstTilVedtaksbrev? = null,
    fnr: Fnr,
    saksbehandlerNavIdent: String,
    beslutterNavIdent: String?,
    innvilgelsesperioder: Innvilgelsesperioder,
    saksnummer: Saksnummer,
    forhåndsvisning: Boolean,
    barnetillegg: Periodisering<AntallBarn>?,
): String {
    val brukersNavn = hentBrukersNavn(fnr)
    val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
    val besluttersNavn = beslutterNavIdent?.let { hentSaksbehandlersNavn(it) }

    return BrevFørstegangsvedtakInnvilgelseDTO(
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
        innvilgelsesperioder = tilInnvilgelsesperioder(innvilgelsesperioder),
        barnetillegg = tilBarnetillegg(barnetillegg),
    ).let { serialize(it) }
}

private fun tilIntroTekst(
    innvilgelsesperioder: Innvilgelsesperioder,
    barnetillegg: Periodisering<AntallBarn>?,
): String {
    val antallDagerPerUkeTekst =
        toAntallDagerTekst(innvilgelsesperioder.antallDagerPerMeldeperiode)?.let { " for $it" } ?: ""

    val perioderMedBarnetillegg = barnetillegg?.perioderMedVerdi
        ?.filter { it.verdi.value > 0 }
        ?.let { it.ifEmpty { null } }

    val sammenhengendeInnvilgelsesperioder = innvilgelsesperioder.perioder.leggSammen(false)

    val tiltakspengerPerioderString = sammenhengendeInnvilgelsesperioder.map {
        "fra og med ${it.fraOgMed.format(norskDatoFormatter)} til og med ${it.tilOgMed.format(norskDatoFormatter)}"
    }.joinMedKonjunksjon()

    val barnetilleggPerioderString = perioderMedBarnetillegg
        ?.map { periodeMedVerdi ->
            val antallBarn = periodeMedVerdi.verdi.toTekst()
            "for $antallBarn barn fra og med ${periodeMedVerdi.periode.fraOgMed.format(norskDatoFormatter)} til og med ${
                periodeMedVerdi.periode.tilOgMed.format(norskDatoFormatter)
            }"
        }?.joinMedKonjunksjon()

    return "Du får tiltakspenger $tiltakspengerPerioderString$antallDagerPerUkeTekst fordi du deltar på arbeidsmarkedstiltak.".plus(
        barnetilleggPerioderString?.let { "\n\nDu får barnetillegg $it." } ?: "",
    )
}

private fun tilInnvilgelsesperioder(
    innvilgelsesperioder: Innvilgelsesperioder,
): List<BrevPeriodeDTO> {
    val sammenhengendeInnvilgelsesperioder = innvilgelsesperioder.perioder.leggSammen(false)

    return sammenhengendeInnvilgelsesperioder.map {
        BrevPeriodeDTO.fraPeriode(it)
    }
}

private fun tilBarnetillegg(
    barnetillegg: Periodisering<AntallBarn>?,
): List<BrevBarnetilleggDTO> {
    val perioderMedBarnetillegg = barnetillegg?.perioderMedVerdi
        ?.filter { it.verdi.value > 0 }
        ?.let { it.ifEmpty { null } }

    return perioderMedBarnetillegg?.map { periodeMedVerdi ->
        BrevBarnetilleggDTO(
            antallBarnTekst = periodeMedVerdi.verdi.toTekst(),
            periode = BrevPeriodeDTO.fraPeriode(periodeMedVerdi.periode),
        )
    } ?: emptyList()
}
