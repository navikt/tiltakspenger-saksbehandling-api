package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Forskrift
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Ledd
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Paragraf
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingUtfall
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

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
    val datoForUtsending: String,
)

internal suspend fun genererAvslagSøknadsbrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    tilleggstekst: FritekstTilVedtaksbrev,
    avslagsgrunner: NonEmptySet<Avslagsgrunnlag>,
    fnr: Fnr,
    saksbehandlerNavIdent: String,
    beslutterNavIdent: String?,
    saksnummer: Saksnummer,
    forhåndsvisning: Boolean,
    harSøktBarnetillegg: Boolean,
    avslagsperiode: Periode,
    datoForUtsending: LocalDate,
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
        datoForUtsending = datoForUtsending.format(norskDatoFormatter),
    ).let { serialize(it) }
}

internal suspend fun Rammevedtak.genererAvslagSøknadsbrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    datoForUtsending: LocalDate,
): String {
    require(behandling is Søknadsbehandling && behandling.utfall is SøknadsbehandlingUtfall.Avslag) {
        "Behandlingen må være et avslag for å generere avslagbrev"
    }

    val brukersNavn = hentBrukersNavn(this.fnr)
    val saksbehandlersNavn = hentSaksbehandlersNavn(this.saksbehandlerNavIdent)
    val besluttersNavn = hentSaksbehandlersNavn(this.beslutterNavIdent)

    val harSøktBarnetillegg = behandling.søknad.barnetillegg.isNotEmpty() ?: false
    return BrevSøknadAvslagDTO(
        personalia = BrevPersonaliaDTO(
            ident = fnr.verdi,
            fornavn = brukersNavn.fornavn,
            etternavn = brukersNavn.mellomnavnOgEtternavn,
        ),
        saksnummer = saksnummer.verdi,
        tilleggstekst = this.behandling.fritekstTilVedtaksbrev!!.verdi,
        forhåndsvisning = false,
        avslagsgrunner = this.behandling.utfall.avslagsgrunner.toAvslagsgrunnerBrevDto(),
        hjemlerTekst = if (this.behandling.utfall.avslagsgrunner.size > 1) {
            this.behandling.utfall.avslagsgrunner.createBrevForskrifter(
                harSøktBarnetillegg,
            )
        } else {
            null
        },
        harSøktMedBarn = harSøktBarnetillegg,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        avslagsgrunnerSize = this.behandling.utfall.avslagsgrunner.size,
        avslagFraOgMed = this.periode.fraOgMed.format(norskDatoFormatter),
        avslagTilOgMed = this.periode.tilOgMed.format(norskDatoFormatter),
        datoForUtsending = datoForUtsending.format(norskDatoFormatter),
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
        .groupBy { it.paragraf.nummer }
        .map { e -> Pair(e.key, e.value.distinct().mapNotNull { it.ledd }.sortedBy { it.nummer }) }
        .sortedBy { it.first }

    val arbeidsmarkedlovenHjemler = hjemler.filter { it.forskrift == Forskrift.Arbeidsmarkedsloven }
        .groupBy { it.paragraf.nummer }
        .map { e -> Pair(e.key, e.value.distinct().mapNotNull { it.ledd }.sortedBy { it.nummer }) }
        .sortedBy { it.first }

    return when {
        arbeidsmarkedlovenHjemler.isNotEmpty() && tiltakspengeHjemler.isEmpty() -> {
            val paragraf = if (arbeidsmarkedlovenHjemler.size == 1) "§" else "§§"
            "Dette kommer frem av arbeidsmarkedsloven $paragraf " + arbeidsmarkedlovenHjemler.joinToString {
                "${it.first}" + if (it.second.isEmpty()) "" else " ${it.second.toLeddTekst()}"
            } + "."
        }

        arbeidsmarkedlovenHjemler.isEmpty() && tiltakspengeHjemler.isNotEmpty() -> {
            val paragraf = if (tiltakspengeHjemler.size == 1) "§" else "§§"
            "Dette kommer frem av tiltakspengeforskriften $paragraf " + tiltakspengeHjemler.joinToString {
                "${it.first}" + if (it.second.isEmpty()) "" else " ${it.second.toLeddTekst()}"
            } + "."
        }

        arbeidsmarkedlovenHjemler.isNotEmpty() && tiltakspengeHjemler.isNotEmpty() -> {
            val paragrafArbeidsmarkedloven = if (arbeidsmarkedlovenHjemler.size == 1) "§" else "§§"
            val paragrafTiltakspenger = if (tiltakspengeHjemler.size == 1) "§" else "§§"
            "Dette kommer frem av arbeidsmarkedsloven $paragrafArbeidsmarkedloven " + arbeidsmarkedlovenHjemler.joinToString {
                "${it.first}" + if (it.second.isEmpty()) "" else " ${it.second.toLeddTekst()}"
            } +
                ", og tiltakspengeforskriften $paragrafTiltakspenger " + tiltakspengeHjemler.joinToString {
                    "${it.first}" + if (it.second.isEmpty()) "" else " ${it.second.toLeddTekst()}"
                } + "."
        }

        else -> throw IllegalStateException("Fant ingen hjemler for avslagsgrunnlag")
    }
}

private fun Ledd.toLeddTekst(): String = when (this.nummer) {
    1 -> "første"
    2 -> "andre"
    3 -> "tredje"
    else -> throw IllegalArgumentException("Fant ikke mapping mellom paragraf og ledd")
}

private fun List<Ledd>.toLeddTekst(): String = when (this.size) {
    0 -> ""
    1 -> "${this[0].toLeddTekst()} ledd"
    2 -> "${this[0].toLeddTekst()} og ${this[1].toLeddTekst()} ledd"
    else -> {
        val alleUnntattSiste = this.dropLast(1).joinToString(", ") { it.toLeddTekst() }
        val siste = this.last().toLeddTekst()
        "$alleUnntattSiste, og $siste ledd"
    }
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
