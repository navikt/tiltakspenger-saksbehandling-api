package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Forskrift
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Paragraf
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

internal data class BrevSøknadAvslagDTO(
    val personalia: BrevPersonaliaDTO,
    val saksnummer: String,
    val saksbehandlerNavn: String,
    val beslutterNavn: String?,
    val tilleggstekst: String?,
    val avslagsgrunnerSize: Int,
    val avslagsgrunner: List<AvslagsgrunnerBrevDto>,
    val harSøktMedBarn: Boolean,
    val hjemlerTekst: String?,
    val forhåndsvisning: Boolean,
    val avslagFraOgMed: String,
    val avslagTilOgMed: String,
)

internal suspend fun genererAvslagSøknadsbrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    tilleggstekst: FritekstTilVedtaksbrev,
    avslagsgrunner: Set<Avslagsgrunnlag>,
    fnr: Fnr,
    saksbehandlerNavIdent: String,
    beslutterNavIdent: String?,
    saksnummer: Saksnummer,
    forhåndsvisning: Boolean,
    harSøktBarnetillegg: Boolean,
    avslagsperiode: Periode,
): String {
    val brukersNavn = hentBrukersNavn(fnr)
    val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
    val besluttersNavn = beslutterNavIdent?.let { hentSaksbehandlersNavn(it) }

    return BrevSøknadAvslagDTO(
        personalia = BrevPersonaliaDTO(
            ident = fnr.verdi,
            fornavn = brukersNavn.fornavn,
            etternavn = brukersNavn.mellomnavnOgEtternavn,
        ),
        saksnummer = saksnummer.verdi,
        tilleggstekst = tilleggstekst.verdi,
        forhåndsvisning = forhåndsvisning,
        avslagsgrunner = avslagsgrunner.toAvslagsgrunnerBrevDto(),
        hjemlerTekst = if (avslagsgrunner.size > 1) avslagsgrunner.createBrevForskrifter(harSøktBarnetillegg) else null,
        harSøktMedBarn = harSøktBarnetillegg,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        avslagsgrunnerSize = avslagsgrunner.size,
        avslagFraOgMed = avslagsperiode.fraOgMed.format(norskDatoFormatter),
        avslagTilOgMed = avslagsperiode.tilOgMed.format(norskDatoFormatter),

    ).let { serialize(it) }
}

enum class AvslagsgrunnerBrevDto {
    DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK,
    ALDER,
    LIVSOPPHOLDYTELSE,
    KVALIFISERINGSPROGRAMMET,
    INTRODUKSJONSPROGRAMMET,
    LØNN_FRA_TILTAKSARRANGØR,
    LØNN_FRA_ANDRE,
    INSTITUSJONSOPPHOLD,
    FREMMET_FOR_SENT,
}

fun Set<Avslagsgrunnlag>.createBrevForskrifter(harSøktBarnetillegg: Boolean): String {
    val hjemler = this.flatMap { it.hjemler }

    val tiltakspengeHjemler = hjemler.filter { it.forskrift == Forskrift.Tiltakspengeforskriften }
        .let {
            if (harSøktBarnetillegg) {
                it + Hjemmel(
                    paragraf = Paragraf(3),
                    forskrift = Forskrift.Tiltakspengeforskriften,
                )
            } else {
                it
            }
        }
        .distinctBy { it.paragraf.nummer }
        .sortedBy { it.paragraf.nummer }

    val arbeidsmarkedlovenHjemler = hjemler.filter { it.forskrift == Forskrift.Arbeidsmarkedsloven }
        .distinctBy { it.paragraf.nummer }
        .sortedBy { it.paragraf.nummer }

    return when {
        arbeidsmarkedlovenHjemler.isNotEmpty() && tiltakspengeHjemler.isEmpty() -> {
            val paragraf = if (arbeidsmarkedlovenHjemler.size == 1) "§" else "§§"
            "Dette kommer frem av arbeidsmarkedsloven $paragraf " + arbeidsmarkedlovenHjemler.joinToString {
                "${it.paragraf.nummer}" + if (it.toLeddTekst().isBlank()) "" else " ${it.toLeddTekst()}"
            } + "."
        }

        arbeidsmarkedlovenHjemler.isEmpty() && tiltakspengeHjemler.isNotEmpty() -> {
            val paragraf = if (tiltakspengeHjemler.size == 1) "§" else "§§"
            "Dette kommer frem av tiltakspengeforskriften $paragraf " + tiltakspengeHjemler.joinToString {
                "${it.paragraf.nummer}" + if (it.toLeddTekst().isBlank()) "" else " ${it.toLeddTekst()}"
            } + "."
        }

        arbeidsmarkedlovenHjemler.isNotEmpty() && tiltakspengeHjemler.isNotEmpty() -> {
            val paragragArbeidsmarkedloven = if (arbeidsmarkedlovenHjemler.size == 1) "§" else "§§"
            val paragrafTiltakspenger = if (tiltakspengeHjemler.size == 1) "§" else "§§"
            "Dette kommer frem av arbeidsmarkedsloven $paragragArbeidsmarkedloven " + arbeidsmarkedlovenHjemler.joinToString {
                "${it.paragraf.nummer}" + if (it.toLeddTekst().isBlank()) "" else " ${it.toLeddTekst()}"
            } +
                ", og tiltakspengeforskriften $paragrafTiltakspenger " + tiltakspengeHjemler.joinToString {
                    "${it.paragraf.nummer}" + if (it.toLeddTekst().isBlank()) "" else " ${it.toLeddTekst()}"
                } + "."
        }

        else -> throw IllegalStateException("Fant ingen hjemler for avslagsgrunnlag")
    }
}

private fun Hjemmel.toLeddTekst(): String = when (this.ledd?.nummer) {
    1 -> "første ledd"
    2 -> "andre ledd"
    3 -> "tredje ledd"
    null -> ""
    else -> throw IllegalArgumentException("Fant ikke mapping mellom paragraf og ledd")
}

fun Avslagsgrunnlag.toAvslagsgrunnerBrevDto(): AvslagsgrunnerBrevDto = when (this) {
    Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak -> AvslagsgrunnerBrevDto.DELTAR_IKKE_PÅ_ARBEIDSMARKEDSTILTAK
    Avslagsgrunnlag.Alder -> AvslagsgrunnerBrevDto.ALDER
    Avslagsgrunnlag.Livsoppholdytelser -> AvslagsgrunnerBrevDto.LIVSOPPHOLDYTELSE
    Avslagsgrunnlag.Kvalifiseringsprogrammet -> AvslagsgrunnerBrevDto.KVALIFISERINGSPROGRAMMET
    Avslagsgrunnlag.Introduksjonsprogrammet -> AvslagsgrunnerBrevDto.INTRODUKSJONSPROGRAMMET
    Avslagsgrunnlag.LønnFraTiltaksarrangør -> AvslagsgrunnerBrevDto.LØNN_FRA_TILTAKSARRANGØR
    Avslagsgrunnlag.LønnFraAndre -> AvslagsgrunnerBrevDto.LØNN_FRA_ANDRE
    Avslagsgrunnlag.Institusjonsopphold -> AvslagsgrunnerBrevDto.INSTITUSJONSOPPHOLD
    Avslagsgrunnlag.FremmetForSent -> AvslagsgrunnerBrevDto.FREMMET_FOR_SENT
}

fun Set<Avslagsgrunnlag>.toAvslagsgrunnerBrevDto(): List<AvslagsgrunnerBrevDto> =
    this.map { it.toAvslagsgrunnerBrevDto() }
