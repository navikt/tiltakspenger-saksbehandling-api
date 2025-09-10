package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelHarIkkeRettighet
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

private data class BrevRevurderingStansDTO(
    override val personalia: BrevPersonaliaDTO,
    override val saksnummer: String,
    override val saksbehandlerNavn: String,
    override val beslutterNavn: String?,
    override val datoForUtsending: String,
    override val tilleggstekst: String?,
    override val forhandsvisning: Boolean,
    val barnetillegg: Boolean = false,
    @Deprecated("rammevedtakFraDato er renamet til virkningsperiodeFraDato, beholdes til pdfgen har fjernet bruken")
    val rammevedtakFraDato: String,
    @Deprecated("rammevedtakTilDato er renamet til virkningsperiodeTilDato, beholdes til pdfgen har fjernet bruken")
    val rammevedtakTilDato: String,
    val virkningsperiodeFraDato: String,
    val virkningsperiodeTilDato: String,
    val kontor: String,
    val valgtHjemmelTekst: List<String>?,
) : BrevRammevedtakBaseDTO

internal suspend fun Rammevedtak.toRevurderingStans(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    vedtaksdato: LocalDate,
): String {
    require(behandling is Revurdering && behandling.resultat is RevurderingResultat.Stans)

    return genererStansbrev(
        hentBrukersNavn = hentBrukersNavn,
        hentSaksbehandlersNavn = hentSaksbehandlersNavn,
        vedtaksdato = vedtaksdato,
        fnr = fnr,
        saksbehandlerNavIdent = saksbehandler,
        beslutterNavIdent = beslutter,
        virkningsperiode = this.periode,
        saksnummer = saksnummer,
        forhåndsvisning = false,
        barnetillegg = barnetillegg != null,
        valgteHjemler = behandling.resultat.valgtHjemmel,
        tilleggstekst = behandling.fritekstTilVedtaksbrev,
    )
}

internal suspend fun genererStansbrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    vedtaksdato: LocalDate,
    fnr: Fnr,
    saksbehandlerNavIdent: String,
    beslutterNavIdent: String?,
    virkningsperiode: Periode,
    saksnummer: Saksnummer,
    forhåndsvisning: Boolean,
    barnetillegg: Boolean,
    valgteHjemler: List<ValgtHjemmelHarIkkeRettighet>? = null,
    tilleggstekst: FritekstTilVedtaksbrev? = null,
): String {
    val brukersNavn = hentBrukersNavn(fnr)
    val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
    val besluttersNavn = beslutterNavIdent?.let { hentSaksbehandlersNavn(it) }

    return BrevRevurderingStansDTO(
        personalia = BrevPersonaliaDTO(
            ident = fnr.verdi,
            fornavn = brukersNavn.fornavn,
            etternavn = brukersNavn.mellomnavnOgEtternavn,
        ),
        rammevedtakFraDato = virkningsperiode.fraOgMed.format(norskDatoFormatter),
        rammevedtakTilDato = virkningsperiode.tilOgMed.format(norskDatoFormatter),
        virkningsperiodeFraDato = virkningsperiode.fraOgMed.format(norskDatoFormatter),
        virkningsperiodeTilDato = virkningsperiode.tilOgMed.format(norskDatoFormatter),
        saksnummer = saksnummer.verdi,
        barnetillegg = barnetillegg,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        // TODO post-mvp: legg inn NORG integrasjon for å hente saksbehandlers kontor.
        kontor = "Nav Tiltakspenger",
        // Dette er vår dato, det brukes typisk når bruker klager på vedtaksbrev på dato ...
        datoForUtsending = vedtaksdato.format(norskDatoFormatter),
        forhandsvisning = forhåndsvisning,
        valgtHjemmelTekst = valgteHjemler?.map { it.tekstVedtaksbrev(barnetillegg) },
        tilleggstekst = tilleggstekst?.verdi,
    ).let { serialize(it) }
}

private fun ValgtHjemmelHarIkkeRettighet.tekstVedtaksbrev(barnetillegg: Boolean): String {
    return when (this as ValgtHjemmelForStans) {
        ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak ->
            if (barnetillegg) {
                "du ikke lenger deltar på arbeidsmarkedstiltak. Du må være deltaker i et arbeidsmarkedstiltak for å ha rett til tiltakspenger og barnetillegg. Dette kommer frem av arbeidsmarkedsloven § 13, tiltakspengeforskriften §§ 2 og 3."
            } else {
                "du ikke lenger deltar på arbeidsmarkedstiltak. Du må være deltaker i et arbeidsmarkedstiltak for å ha rett til å få tiltakspenger. Dette kommer frem av arbeidsmarkedsloven § 13 og tiltakspengeforskriften § 2."
            }

        ValgtHjemmelForStans.Alder ->
            "du ikke har fylt 18 år. Du må ha fylt 18 år for å ha rett til å få tiltakspenger. Det kommer frem av tiltakspengeforskriften § 3."

        ValgtHjemmelForStans.Livsoppholdytelser ->
            if (barnetillegg) {
                "du mottar en annen pengestøtte til livsopphold. Deltakere som har rett til andre pengestøtter til livsopphold har ikke samtidig rett til å få tiltakspenger og barnetillegg. Dette kommer frem av arbeidsmarkedsloven § 13 første ledd og forskrift om tiltakspenger §§ 3 og 7."
            } else {
                "du mottar en annen pengestøtte til livsopphold. Deltakere som har rett til andre pengestøtter til livsopphold, har ikke samtidig rett til å få tiltakspenger. Dette kommer frem av arbeidsmarkedsloven § 13 første ledd og tiltakspengeforskriften § 7."
            }

        ValgtHjemmelForStans.Kvalifiseringsprogrammet ->
            if (barnetillegg) {
                "du deltar på kvalifiseringsprogram. Deltakere i kvalifiseringsprogram, har ikke rett til tiltakspenger og barnetillegg. Dette kommer frem av tiltakspengeforskriften §§ 3 og 7 tredje ledd."
            } else {
                "du deltar på kvalifiseringsprogram. Deltakere i kvalifiseringsprogram, har ikke rett til tiltakspenger. Dette kommer frem av tiltakspengeforskriften § 7 tredje ledd."
            }

        ValgtHjemmelForStans.Introduksjonsprogrammet ->
            if (barnetillegg) {
                "du deltar på introduksjonsprogram. Deltakere i introduksjonsprogram, har ikke rett til tiltakspenger og barnetillegg. Dette kommer frem av tiltakspengeforskriften §§ 3 og 7 tredje ledd."
            } else {
                "du deltar på introduksjonsprogram. Deltakere i introduksjonsprogram, har ikke rett til tiltakspenger. Dette kommer frem av tiltakspengeforskriften § 7 tredje ledd."
            }

        ValgtHjemmelForStans.LønnFraTiltaksarrangør ->
            if (barnetillegg) {
                "du mottar lønn fra tiltaksarrangør for tiden i arbeidsmarkedstiltaket. Deltakere som mottar lønn fra tiltaksarrangør for tid i arbeidsmarkedstiltaket, har ikke rett til tiltakspenger og barnetillegg. Dette kommer frem av tiltakspengeforskriften §§ 3 og 8. "
            } else {
                "du mottar lønn fra tiltaksarrangør for tiden i arbeidsmarkedstiltaket. Deltakere som mottar lønn fra tiltaksarrangør for tid i arbeidsmarkedstiltaket, har ikke rett til tiltakspenger. Dette kommer frem av tiltakspengeforskriften § 8."
            }

        ValgtHjemmelForStans.LønnFraAndre ->
            if (barnetillegg) {
                """
                    du mottar lønn for arbeid som er en del av tiltaksdeltakelsen og du derfor har dekning av utgifter til livsopphold.
                    Deltaker i arbeidsmarkedstiltak som har rett til å få dekket utgifter til livsopphold på annen måte har ikke rett til tiltakspenger og barnetillegg. Lønn anses som dekning av utgifter til livsopphold på annen måte, når du får lønnen for arbeid som er en del av tiltaksdeltakelsen.
                    Lønn fra arbeid utenom tiltaksdeltakelsen har ikke betydning for din rett til tiltakspenger.
                    Dette kommer frem av arbeidsmarkedsloven § 13 og tiltakspengeforskriften §$ 3 og 8 andre ledd.
                """
            } else {
                """
                    du mottar lønn for arbeid som er en del av tiltaksdeltakelsen og du derfor har dekning av utgifter til livsopphold.
                    Deltaker i arbeidsmarkedstiltak som har rett til å få dekket utgifter til livsopphold på annen måte har ikke rett til tiltakspenger. Lønn anses som dekning av utgifter til livsopphold på annen måte, når du får lønnen for arbeid som er en del av tiltaksdeltakelsen.
                    Lønn fra arbeid utenom tiltaksdeltakelsen har ikke betydning for din rett til tiltakspenger.
                    Dette kommer frem av arbeidsmarkedsloven § 13 og tiltakspengeforskriften § 8 andre ledd.
                """
            }

        ValgtHjemmelForStans.Institusjonsopphold ->
            if (barnetillegg) {
                """
                    du oppholder deg på en institusjon med gratis opphold, mat og drikke. 
                    Deltakere som har opphold i institusjon, med gratis opphold, mat og drikke. under gjennomføringen av arbeidsmarkedstiltaket, har ikke rett til tiltakspenger og barnetillegg.
                    Det er gjort unntak for opphold i barneverns-institusjoner. Dette kommer frem av tiltakspengeforskriften §§ 3 og 9. 
                """
            } else {
                """
                    du oppholder deg på en institusjon med gratis opphold, mat og drikke. 
                    Deltakere som har opphold i institusjon, med gratis opphold, mat og drikke. under gjennomføringen av arbeidsmarkedstiltaket, har ikke rett til tiltakspenger.
                    Det er gjort unntak for opphold i barneverns-institusjoner. Dette kommer frem av tiltakspengeforskriften § 9. 
                """
            }
    }.trimIndent()
}
