package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Forskrift
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
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

// TODO raq - test
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

// TODO raq - test
fun Set<Avslagsgrunnlag>.createBrevForskrifter(harSøktBarnetillegg: Boolean): String {
    val hjemler = this.flatMap { it.hjemler }

    val tiltakspengeHjemler = hjemler.filter { it.forskrift == Forskrift.Tiltakspengeforskriften }
        .map { it.paragraf.nummer }
        .let {
            if (harSøktBarnetillegg) {
                it + 3
            } else {
                it
            }
        }
        .distinct()
        .sorted()

    val arbeidsmarkedlovenHjemler = hjemler.filter { it.forskrift == Forskrift.Arbeidsmarkedsloven }
        .map { it.paragraf.nummer }
        .distinct()
        .sorted()

    return when {
        arbeidsmarkedlovenHjemler.isNotEmpty() && tiltakspengeHjemler.isEmpty() -> {
            val paragraf = if (arbeidsmarkedlovenHjemler.size == 1) "§" else "§§"
            "Dette kommer frem av arbeidsmarkedsloven $paragraf " + arbeidsmarkedlovenHjemler.joinToString { it.toString() } + "."
        }

        arbeidsmarkedlovenHjemler.isEmpty() && tiltakspengeHjemler.isNotEmpty() -> {
            val paragraf = if (tiltakspengeHjemler.size == 1) "§" else "§§"
            "Dette kommer frem av tiltakspengeforskriften $paragraf " + tiltakspengeHjemler.joinToString { it.toString() } + "."
        }

        arbeidsmarkedlovenHjemler.isNotEmpty() && tiltakspengeHjemler.isNotEmpty() -> {
            val paragragArbeidsmarkedloven = if (arbeidsmarkedlovenHjemler.size == 1) "§" else "§§"
            val paragrafTiltakspenger = if (tiltakspengeHjemler.size == 1) "§" else "§§"
            "Dette kommer frem av arbeidsmarkedsloven $paragragArbeidsmarkedloven " + arbeidsmarkedlovenHjemler.joinToString { it.toString() } +
                ", tiltakspengeforskriften $paragrafTiltakspenger " + tiltakspengeHjemler.joinToString { it.toString() } + "."
        }

        else -> throw IllegalStateException("Fant ingen hjemler for avslagsgrunnlag")
    }
}

// TODO raq - test
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

// TODO raq - test
fun Set<Avslagsgrunnlag>.toAvslagsgrunnerBrevDto(): List<AvslagsgrunnerBrevDto> =
    this.map { it.toAvslagsgrunnerBrevDto() }
