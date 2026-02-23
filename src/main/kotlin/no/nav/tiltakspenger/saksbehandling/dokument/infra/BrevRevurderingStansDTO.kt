package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
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
    val stansFraOgMedDato: String,
    val valgtHjemmelTekst: List<String>?,
    val barnetillegg: Boolean,
) : BrevRammevedtakBaseDTO

suspend fun Rammevedtak.toRevurderingStans(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    vedtaksdato: LocalDate,
    harStansetBarnetillegg: Boolean,
): String {
    require(rammebehandling is Revurdering && rammebehandling.resultat is Revurderingsresultat.Stans)

    return genererStansbrev(
        hentBrukersNavn = hentBrukersNavn,
        hentSaksbehandlersNavn = hentSaksbehandlersNavn,
        vedtaksdato = vedtaksdato,
        fnr = fnr,
        saksbehandlerNavIdent = saksbehandler,
        beslutterNavIdent = beslutter,
        stansperiode = this.periode,
        saksnummer = saksnummer,
        forhåndsvisning = false,
        valgteHjemler = rammebehandling.resultat.valgtHjemmel!!,
        tilleggstekst = rammebehandling.fritekstTilVedtaksbrev,
        harStansetBarnetillegg = harStansetBarnetillegg,
    )
}

suspend fun genererStansbrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    vedtaksdato: LocalDate,
    fnr: Fnr,
    saksbehandlerNavIdent: String,
    beslutterNavIdent: String?,
    stansperiode: Periode,
    saksnummer: Saksnummer,
    forhåndsvisning: Boolean,
    valgteHjemler: NonEmptySet<HjemmelForStans>,
    tilleggstekst: FritekstTilVedtaksbrev? = null,
    harStansetBarnetillegg: Boolean,
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
        stansFraOgMedDato = stansperiode.fraOgMed.format(norskDatoFormatter),
        saksnummer = saksnummer.verdi,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        // Dette er vår dato, det brukes typisk når bruker klager på vedtaksbrev på dato ...
        datoForUtsending = vedtaksdato.format(norskDatoFormatter),
        forhandsvisning = forhåndsvisning,
        valgtHjemmelTekst = when (harStansetBarnetillegg) {
            true -> valgteHjemler.map { it.tekstMedBarnetillegg() }
            false -> valgteHjemler.map { it.tekstUtenBarnetillegg() }
        },
        tilleggstekst = tilleggstekst?.verdi,
        barnetillegg = harStansetBarnetillegg,
    ).let { serialize(it) }
}

private fun HjemmelForStans.tekstUtenBarnetillegg(): String {
    return when (this) {
        HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak ->
            """
                du ikke lenger deltar på arbeidsmarkedstiltak.

                Du må være deltaker i et arbeidsmarkedstiltak for å ha rett til å få tiltakspenger.
                
                Dette kommer frem av arbeidsmarkedsloven § 13 og tiltakspengeforskriften § 2.
            """

        HjemmelForStans.Alder ->
            """
                du ikke har fylt 18 år. Du må ha fylt 18 år for å ha rett til å få tiltakspenger. 

                Det kommer frem av tiltakspengeforskriften § 3.
            """

        HjemmelForStans.Livsoppholdytelser ->
            """
                du mottar en annen pengestøtte til livsopphold. Deltakere som har rett til andre pengestøtter til livsopphold, har ikke samtidig rett til å få tiltakspenger. 
                
                Dette kommer frem av arbeidsmarkedsloven § 13 første ledd og tiltakspengeforskriften § 7 første ledd.
            """

        HjemmelForStans.Kvalifiseringsprogrammet ->
            """
                du deltar på kvalifiseringsprogram. Deltakere i kvalifiseringsprogram, har ikke rett til tiltakspenger. 
                
                Dette kommer frem av tiltakspengeforskriften § 7 tredje ledd.                 
            """

        HjemmelForStans.Introduksjonsprogrammet ->
            """
                du deltar på introduksjonsprogram. Deltakere i introduksjonsprogram, har ikke rett til tiltakspenger.
                
                Dette kommer frem av tiltakspengeforskriften § 7 og 3 tredje ledd.
            """

        HjemmelForStans.LønnFraTiltaksarrangør ->
            """
                du mottar lønn fra tiltaksarrangør for tiden i arbeidsmarkedstiltaket. 

                Deltakere som mottar lønn fra tiltaksarrangør for tid i arbeidsmarkedstiltaket, har ikke rett til tiltakspenger.

                Dette kommer frem av tiltakspengeforskriften § 8.
            """

        HjemmelForStans.LønnFraAndre ->
            """
                du mottar lønn for arbeid som er en del av tiltaksdeltakelsen og du derfor har dekning av utgifter til livsopphold.
                
                Deltaker i arbeidsmarkedstiltak som har rett til å få dekket utgifter til livsopphold på annen måte har ikke rett til tiltakspenger. Lønn anses som dekning av utgifter til livsopphold på annen måte, når du får lønnen for arbeid som er en del av tiltaksdeltakelsen.
                
                Lønn fra arbeid utenom tiltaksdeltakelsen har ikke betydning for din rett til tiltakspenger.
                
                Dette kommer frem av arbeidsmarkedsloven § 13 og tiltakspengeforskriften § 8 andre ledd.
            """

        HjemmelForStans.Institusjonsopphold ->
            """                	
                du oppholder deg på en institusjon med gratis opphold, mat og drikke. 
                
                Deltakere som har opphold i institusjon, med gratis opphold, mat og drikke under gjennomføringen av arbeidsmarkedstiltaket, har ikke rett til tiltakspenger.
                
                Det er gjort unntak for opphold i  barneverns-institusjoner.  Dette kommer frem av tiltakspengeforskriften § 9. 
            """

        HjemmelForStans.IkkeLovligOpphold ->
            """
                du i denne perioden ikke har lovlig opphold i Norge. 
                
                Du må ha lovlig opphold i Norge, for å ha rett til tiltakspenger.
                
                Dette kommer frem av arbeidsmarkedsloven § 2.                
            """
    }.trimIndent()
}

private fun HjemmelForStans.tekstMedBarnetillegg(): String {
    return when (this) {
        HjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak ->
            """
                du må være deltaker i et arbeidsmarkedstiltak for å ha rett til tiltakspenger og barnetillegg.
                
                Dette kommer frem av arbeidsmarkedsloven § 13, tiltakspengeforskriften § 2 og 3.
                
                Dette kommer frem av arbeidsmarkedsloven § 13 og tiltakspengeforskriften § 2.
            """

        HjemmelForStans.Alder ->
            """
                du ikke har fylt 18 år. Du må ha fylt 18 år for å ha rett til å få tiltakspenger og barnetillegg. 

                Det kommer frem av tiltakspengeforskriften § 3.
            """

        HjemmelForStans.Livsoppholdytelser ->
            """
                du mottar en annen pengestøtte til livsopphold. Deltakere som har rett til andre pengestøtter til livsopphold har ikke samtidig rett til å få tiltakspenger og barnetillegg.
                
                Dette kommer frem av arbeidsmarkedsloven § 13 første ledd, forskrift om tiltakspenger § 7 første ledd.
            """

        HjemmelForStans.Kvalifiseringsprogrammet ->
            """
                du deltar på kvalifiseringsprogram. Deltakere i kvalifiseringsprogram, har ikke rett til tiltakspenger og barnetillegg.
                
                Dette kommer frem av tiltakspengeforskriften § 7 tredje ledd.             
            """

        HjemmelForStans.Introduksjonsprogrammet ->
            """
                du deltar på introduksjonsprogram. Deltakere i introduksjonsprogram, har ikke rett til tiltakspenger og barnetillegg.
                
                Dette kommer frem av tiltakspengeforskriften § 7 tredje ledd.
            """

        HjemmelForStans.LønnFraTiltaksarrangør ->
            """
                du mottar lønn fra tiltaksarrangør for tiden i arbeidsmarkedstiltaket 
                
                Deltakere som mottar lønn fra tiltaksarrangør for tid i arbeidsmarkedstiltaket, har ikke rett til tiltakspenger og barnetillegg.
                
                Dette kommer frem av tiltakspengeforskriften § 8.
            """

        HjemmelForStans.LønnFraAndre ->
            """
                du mottar lønn for arbeid som er en del av tiltaksdeltakelsen og du derfor har dekning av utgifter til livsopphold.
                
                Deltaker i arbeidsmarkedstiltak som har rett til å få dekket utgifter til livsopphold på annen måte har ikke rett til tiltakspenger og barnetillegg. Lønn anses som dekning av utgifter til livsopphold på annen måte, når du får lønnen for arbeid som er en del av tiltaksdeltakelsen.
                
                Lønn fra arbeid utenom tiltaksdeltakelsen har ikke betydning for din rett til tiltakspenger.
                
                Dette kommer frem av arbeidsmarkedsloven § 13, tiltakspengeforskriften § 3, § 8 andre ledd.
            """

        HjemmelForStans.Institusjonsopphold ->
            """                	
                du oppholder deg på en institusjon med gratis opphold, mat og drikke. 
                
                Deltakere som har opphold i institusjon, med gratis opphold, mat og drikke under gjennomføringen av arbeidsmarkedstiltaket, har ikke rett til tiltakspenger og barnetillegg.
                
                Det er gjort unntak for opphold i barneverns-institusjoner. Dette kommer frem av tiltakspengeforskriften §3, §9.
            """

        HjemmelForStans.IkkeLovligOpphold ->
            """
                du i denne perioden ikke har lovlig opphold i Norge. 
                
                Du må ha lovlig opphold i Norge, for å ha rett til tiltakspenger og barnetillegg.
                
                Dette kommer frem av arbeidsmarkedsloven § 2.                
            """
    }.trimIndent()
}
