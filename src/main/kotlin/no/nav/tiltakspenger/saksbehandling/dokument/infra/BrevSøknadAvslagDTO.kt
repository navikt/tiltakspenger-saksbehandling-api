package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Ledd
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rettskilde
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

private data class BrevSøknadAvslagDTO(
    override val personalia: BrevPersonaliaDTO,
    override val saksnummer: String,
    override val saksbehandlerNavn: String,
    override val beslutterNavn: String?,
    override val datoForUtsending: String,
    override val tilleggstekst: String?,
    override val forhandsvisning: Boolean,
    // TODO: Fjern når tiltakspenger-pdfgenrs leser valgtHjemmelTekst i prod; eldre malversjoner forgrener fortsatt på enumen.
    val avslagsgrunner: List<AvslagsgrunnerBrevDto>,
    val valgtHjemmelTekst: List<String>,
    val harSøktMedBarn: Boolean,
    val hjemlerTekst: String?,
    val avslagFraOgMed: String,
    val avslagTilOgMed: String,
) : BrevRammevedtakBaseDTO

suspend fun genererAvslagSøknadsbrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    tilleggstekst: FritekstTilVedtaksbrev?,
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
        tilleggstekst = tilleggstekst?.verdi,
        forhandsvisning = forhåndsvisning,
        avslagsgrunner = avslagsgrunner.toAvslagsgrunnerBrevDto(),
        valgtHjemmelTekst = avslagsgrunner.tilValgtHjemmelTekst(harSøktBarnetillegg),
        hjemlerTekst = if (avslagsgrunner.size > 1) avslagsgrunner.createBrevForskrifter(harSøktBarnetillegg) else null,
        harSøktMedBarn = harSøktBarnetillegg,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        avslagFraOgMed = avslagsperiode.fraOgMed.format(norskDatoFormatter),
        avslagTilOgMed = avslagsperiode.tilOgMed.format(norskDatoFormatter),
        datoForUtsending = datoForUtsending.format(norskDatoFormatter),
    ).let { serialize(it) }
}

suspend fun Rammevedtak.genererAvslagSøknadsbrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    datoForUtsending: LocalDate,
): String {
    require(rammebehandling is Søknadsbehandling && rammebehandling.resultat is Søknadsbehandlingsresultat.Avslag) {
        "Behandlingen må være et avslag for å generere avslagbrev"
    }

    return genererAvslagSøknadsbrev(
        hentBrukersNavn = hentBrukersNavn,
        hentSaksbehandlersNavn = hentSaksbehandlersNavn,
        tilleggstekst = rammebehandling.fritekstTilVedtaksbrev,
        avslagsgrunner = rammebehandling.resultat.avslagsgrunner,
        fnr = fnr,
        saksbehandlerNavIdent = saksbehandler,
        beslutterNavIdent = beslutter,
        saksnummer = saksnummer,
        forhåndsvisning = false,
        harSøktBarnetillegg = rammebehandling.søknad.barnetillegg.isNotEmpty(),
        avslagsperiode = this.periode,
        datoForUtsending = datoForUtsending,
    )
}

/**
 * Én avslagsgrunn gir full brevtekst med hjemler, som fullfører malens «Du får ikke … fordi ».
 * Flere avslagsgrunner gir én punkttekst per grunn; hjemlene for alle står da samlet i `hjemlerTekst`.
 */
private fun Set<Avslagsgrunnlag>.tilValgtHjemmelTekst(harSøktBarnetillegg: Boolean): List<String> =
    if (this.size == 1) {
        listOf(this.single().tilTekst(harSøktBarnetillegg))
    } else {
        this.map { it.tilPunkttekst(harSøktBarnetillegg) }
    }

private fun Avslagsgrunnlag.tilTekst(medBarnetillegg: Boolean): String {
    val tiltakspengerOgKanskjeBarnetillegg = "tiltakspenger${if (medBarnetillegg) " og barnetillegg" else ""}"

    return when (this) {
        Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak ->
            """
                du ikke deltar på arbeidsmarkedstiltak som gir rett til tiltakspenger.

                For å få $tiltakspengerOgKanskjeBarnetillegg må du delta i arbeidsmarkedstiltak som gir rett til $tiltakspengerOgKanskjeBarnetillegg.

                Dette kommer frem av arbeidsmarkedsloven § 13 og tiltakspengeforskriften ${if (medBarnetillegg) "§§ 2 og 3" else "§ 2"}.
            """

        Avslagsgrunnlag.Alder ->
            """
                du ikke har fylt 18 år. Du må ha fylt 18 år for å ha rett til å få $tiltakspengerOgKanskjeBarnetillegg.

                Dette kommer frem av tiltakspengeforskriften § 3.
            """

        Avslagsgrunnlag.Livsoppholdytelser ->
            """
                du mottar en annen pengestøtte til livsopphold. Deltakere som har rett til andre pengestøtter til livsopphold har ikke samtidig rett til å få $tiltakspengerOgKanskjeBarnetillegg.

                Dette kommer frem av arbeidsmarkedsloven § 13 første ledd og tiltakspengeforskriften § 7 første ledd.
            """

        Avslagsgrunnlag.Kvalifiseringsprogrammet ->
            """
                du deltar på kvalifiseringsprogram. Deltakere i kvalifiseringsprogram har ikke rett til $tiltakspengerOgKanskjeBarnetillegg.

                Dette kommer frem av tiltakspengeforskriften § 7 tredje ledd.
            """

        Avslagsgrunnlag.Introduksjonsprogrammet ->
            """
                du deltar på introduksjonsprogram. Deltakere i introduksjonsprogram har ikke rett til $tiltakspengerOgKanskjeBarnetillegg.

                Dette kommer frem av tiltakspengeforskriften § 7 tredje ledd.
            """

        Avslagsgrunnlag.LønnFraTiltaksarrangør ->
            """
                du mottar lønn fra tiltaksarrangør for tiden i arbeidsmarkedstiltaket.

                Deltakere som mottar lønn fra tiltaksarrangør for tid i arbeidsmarkedstiltaket har ikke rett til $tiltakspengerOgKanskjeBarnetillegg.

                Dette kommer frem av tiltakspengeforskriften § 8.
            """

        Avslagsgrunnlag.LønnFraAndre ->
            """
                du mottar lønn for arbeid som er en del av tiltaksdeltakelsen og du derfor har dekning av utgifter til livsopphold.

                Deltaker i arbeidsmarkedstiltak som har rett til å få dekket utgifter til livsopphold på annen måte har ikke rett til $tiltakspengerOgKanskjeBarnetillegg. Lønn anses som dekning av utgifter til livsopphold på annen måte, når du får lønnen for arbeid som er en del av tiltaksdeltakelsen.

                Lønn fra arbeid utenom tiltaksdeltakelsen har ikke betydning for din rett til tiltakspenger.

                Dette kommer frem av arbeidsmarkedsloven § 13 og tiltakspengeforskriften § 8 andre ledd.
            """

        Avslagsgrunnlag.Institusjonsopphold ->
            """
                du oppholder deg på en institusjon med gratis opphold, mat og drikke.

                Deltakere som har opphold i institusjon med gratis opphold, mat og drikke under gjennomføringen av arbeidsmarkedstiltaket har ikke rett til $tiltakspengerOgKanskjeBarnetillegg.

                Det er gjort unntak for opphold i barnevernsinstitusjoner. Dette kommer frem av tiltakspengeforskriften § 9.
            """

        Avslagsgrunnlag.FremmetForSent ->
            """
                du har søkt om $tiltakspengerOgKanskjeBarnetillegg for sent.

                Tiltakspenger gis for opptil tre måneder før den måneden tiltaksdeltakeren søkte om $tiltakspengerOgKanskjeBarnetillegg.

                Dette kommer frem av tiltakspengeforskriften § 11.
            """
    }.trimIndent()
}

/** Kortere tekst uten hjemler til punktlisten; første linje er selve punktet, resten forklarer det. */
private fun Avslagsgrunnlag.tilPunkttekst(medBarnetillegg: Boolean): String {
    val tiltakspengerOgKanskjeBarnetillegg = "tiltakspenger${if (medBarnetillegg) " og barnetillegg" else ""}"

    return when (this) {
        Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak ->
            """
                Du deltar ikke på arbeidsmarkedstiltak som gir rett til tiltakspenger.
                For å få $tiltakspengerOgKanskjeBarnetillegg må du delta i arbeidsmarkedstiltak som gir rett til $tiltakspengerOgKanskjeBarnetillegg.
            """

        Avslagsgrunnlag.Alder ->
            """
                Du har ikke fylt 18 år.
                Du må ha fylt 18 år for å ha rett til å få $tiltakspengerOgKanskjeBarnetillegg.
            """

        Avslagsgrunnlag.Livsoppholdytelser ->
            """
                Du mottar en annen pengestøtte til livsopphold.
                Deltakere som har rett til andre pengestøtter til livsopphold har ikke samtidig rett til å få $tiltakspengerOgKanskjeBarnetillegg.
            """

        Avslagsgrunnlag.Kvalifiseringsprogrammet ->
            """
                Du deltar på kvalifiseringsprogram.
                Deltakere i kvalifiseringsprogram har ikke rett til $tiltakspengerOgKanskjeBarnetillegg.
            """

        Avslagsgrunnlag.Introduksjonsprogrammet ->
            """
                Du deltar på introduksjonsprogram.
                Deltakere i introduksjonsprogram har ikke rett til $tiltakspengerOgKanskjeBarnetillegg.
            """

        Avslagsgrunnlag.LønnFraTiltaksarrangør ->
            """
                Du mottar lønn fra tiltaksarrangør for tiden i arbeidsmarkedstiltaket.
                Deltakere som mottar lønn fra tiltaksarrangør for tid i arbeidsmarkedstiltaket har ikke rett til $tiltakspengerOgKanskjeBarnetillegg.
            """

        Avslagsgrunnlag.LønnFraAndre ->
            """
                Du mottar lønn for arbeid som er en del av tiltaksdeltakelsen og du derfor har dekning av utgifter til livsopphold.
                Deltaker i arbeidsmarkedstiltak som har rett til å få dekket utgifter til livsopphold på annen måte har ikke rett til $tiltakspengerOgKanskjeBarnetillegg. Lønn anses som dekning av utgifter til livsopphold på annen måte, når du får lønnen for arbeid som er en del av tiltaksdeltakelsen.
                Lønn fra arbeid utenom tiltaksdeltakelsen har ikke betydning for din rett til tiltakspenger.
            """

        Avslagsgrunnlag.Institusjonsopphold ->
            """
                Du oppholder deg på en institusjon med gratis opphold, mat og drikke.
                Deltakere som har opphold i institusjon med gratis opphold, mat og drikke under gjennomføringen av arbeidsmarkedstiltaket har ikke rett til $tiltakspengerOgKanskjeBarnetillegg.
            """

        Avslagsgrunnlag.FremmetForSent ->
            """
                Du har søkt om $tiltakspengerOgKanskjeBarnetillegg for sent.
                Tiltakspenger gis for opptil tre måneder før den måneden tiltaksdeltakeren søkte om $tiltakspengerOgKanskjeBarnetillegg.
            """
    }.trimIndent()
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

    val tiltakspengeHjemler = hjemler.filter { it.rettskilde == Rettskilde.Tiltakspengeforskriften }
        .let {
            if (harSøktBarnetillegg) {
                it + Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_3
            } else {
                it
            }
        }
        .groupBy { it.paragraf.verdi }
        .map { e -> Pair(e.key, e.value.distinct().mapNotNull { it.ledd }.sortedBy { it.nummer }) }
        .sortedBy { it.first }

    val arbeidsmarkedlovenHjemler = hjemler.filter { it.rettskilde == Rettskilde.Arbeidsmarkedsloven }
        .groupBy { it.paragraf.verdi }
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
