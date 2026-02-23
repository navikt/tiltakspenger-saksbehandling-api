package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.NonEmptySet
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.person.Navn
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

private data class BrevOmgjøringOpphørDTO(
    override val personalia: BrevPersonaliaDTO,
    override val saksnummer: String,
    override val saksbehandlerNavn: String,
    override val beslutterNavn: String?,
    override val datoForUtsending: String,
    override val tilleggstekst: String?,
    override val forhandsvisning: Boolean,
    val vedtaksperiode: BrevPeriodeDTO,
    val valgtHjemmelTekst: List<String>?,
    val barnetillegg: Boolean,
) : BrevRammevedtakBaseDTO

suspend fun Rammevedtak.tilBrevOmgjøringOpphørDTO(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    harOpphørtBarnetillegg: Boolean,
    vedtaksdato: LocalDate,
): String {
    require(rammebehandling is Revurdering && rammebehandling.resultat is Omgjøringsresultat.OmgjøringOpphør)

    return genererOpphørBrev(
        hentBrukersNavn = hentBrukersNavn,
        hentSaksbehandlersNavn = hentSaksbehandlersNavn,
        vedtaksdato = vedtaksdato,
        fnr = fnr,
        saksbehandlerNavIdent = saksbehandler,
        beslutterNavIdent = beslutter,
        saksnummer = saksnummer,
        forhåndsvisning = false,
        vedtaksperiode = this.periode,
        valgteHjemler = rammebehandling.resultat.valgteHjemler,
        tilleggstekst = rammebehandling.fritekstTilVedtaksbrev,
        harOpphørtBarnetillegg = harOpphørtBarnetillegg,
    )
}

suspend fun genererOpphørBrev(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    harOpphørtBarnetillegg: Boolean,
    vedtaksdato: LocalDate,
    fnr: Fnr,
    saksbehandlerNavIdent: String,
    beslutterNavIdent: String?,
    saksnummer: Saksnummer,
    forhåndsvisning: Boolean,
    vedtaksperiode: Periode,
    valgteHjemler: NonEmptySet<HjemmelForOpphør>,
    tilleggstekst: FritekstTilVedtaksbrev? = null,
): String {
    val brukersNavn = hentBrukersNavn(fnr)
    val saksbehandlersNavn = hentSaksbehandlersNavn(saksbehandlerNavIdent)
    val besluttersNavn = beslutterNavIdent?.let { hentSaksbehandlersNavn(it) }

    val valgteHjemlerTekst = valgteHjemler.mapNotNull {
        if (Omgjøringsresultat.OmgjøringOpphør.hjemlerSomMåHaFritekst.contains(it)) {
            null
        } else {
            it.tilTekst(harOpphørtBarnetillegg)
        }
    }.toNonEmptyListOrNull()

    require(valgteHjemlerTekst != null || tilleggstekst != null) {
        "For opphørsbrev må det være enten valgte hjemler med tekst eller en tilleggstekst - hjemler: $valgteHjemler"
    }

    return BrevOmgjøringOpphørDTO(
        personalia = BrevPersonaliaDTO(
            ident = fnr.verdi,
            fornavn = brukersNavn.fornavn,
            etternavn = brukersNavn.mellomnavnOgEtternavn,
        ),
        vedtaksperiode = BrevPeriodeDTO.fraPeriode(vedtaksperiode),
        saksnummer = saksnummer.verdi,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        // Dette er vår dato, det brukes typisk når bruker klager på vedtaksbrev på dato ...
        datoForUtsending = vedtaksdato.format(norskDatoFormatter),
        forhandsvisning = forhåndsvisning,
        valgtHjemmelTekst = valgteHjemlerTekst,
        tilleggstekst = tilleggstekst?.verdi,
        barnetillegg = harOpphørtBarnetillegg,
    ).let { serialize(it) }
}

private fun HjemmelForOpphør.tilTekst(medBarnetillegg: Boolean): String? {
    val tiltakspengerOgKanskjeBarnetillegg = "tiltakspenger${if (medBarnetillegg) " og barnetillegg" else ""}"

    return when (this) {
        HjemmelForOpphør.DeltarIkkePåArbeidsmarkedstiltak ->
            """
                Vilkåret om deltakelse i arbeidsmarkedstiltak er ikke oppfylt i denne perioden.
                
                Du må være deltaker i et arbeidsmarkedstiltak for å ha rett til å få $tiltakspengerOgKanskjeBarnetillegg.
                
                Dette kommer frem av arbeidsmarkedsloven § 13, tiltakspengeforskriften § 2.
            """.trimIndent()

        HjemmelForOpphør.Livsoppholdytelser ->
            """
                Du har rett til annen pengestøtte til livsopphold i denne perioden.
                
                Deltakere som har rett til andre pengestøtter til livsopphold, har ikke samtidig rett til å få $tiltakspengerOgKanskjeBarnetillegg.
                
                Dette kommer frem av arbeidsmarkedsloven § 13 første ledd, forskrift om tiltakspenger § 7 første ledd.
            """.trimIndent()

        HjemmelForOpphør.Kvalifiseringsprogrammet ->
            """
                Du er deltaker i kvalifiseringsprogram i denne perioden. Deltakere i kvalifiseringsprogram, har ikke rett til $tiltakspengerOgKanskjeBarnetillegg.
                
                Dette kommer frem av tiltakspengeforskriften § 7 tredje ledd.
            """.trimIndent()

        HjemmelForOpphør.Introduksjonsprogrammet ->
            """
                Du er deltaker i introduksjonsprogram denne perioden. Deltakere i introduksjonsprogram, har ikke rett til $tiltakspengerOgKanskjeBarnetillegg.
                
                Dette kommer frem av tiltakspengeforskriften § 7 tredje ledd.
            """.trimIndent()

        HjemmelForOpphør.LønnFraTiltaksarrangør ->
            """
                Du mottar lønn fra tiltaksarrangøren for tiden i arbeidsmarkedstiltaket for denne perioden.
                
                Deltakere som mottar lønn fra tiltaksarrangør for tid i arbeidsmarkedstiltaket, har ikke rett til $tiltakspengerOgKanskjeBarnetillegg.
                
                Dette kommer frem av tiltakspengeforskriften § 8.
            """.trimIndent()

        HjemmelForOpphør.LønnFraAndre ->
            """
                Du mottar i denne perioden lønn for arbeid som er en del av tiltaksdeltakelsen. Du har derfor dekning av utgifter til livsopphold.
                
                Deltaker i arbeidsmarkedstiltak som har rett til å få dekket utgifter til livsopphold på annen måte har ikke rett til $tiltakspengerOgKanskjeBarnetillegg. Lønn anses som dekning av utgifter til livsopphold på annen måte, når du får lønnen for arbeid som er en del av tiltaksdeltakelsen.
                
                Lønn fra arbeid utenom tiltaksdeltakelsen har ikke betydning for din rett til tiltakspenger.
                
                Dette kommer frem av arbeidsmarkedsloven § 13, tiltakspengeforskriften § 8 andre ledd.
            """.trimIndent()

        HjemmelForOpphør.Institusjonsopphold ->
            """
                Du oppholder deg på en institusjon med gratis opphold, mat og drikke i denne perioden.
                
                Deltakere som har opphold i institusjon med gratis opphold, mat og drikke under gjennomføringen av arbeidsmarkedstiltaket, har ikke rett til $tiltakspengerOgKanskjeBarnetillegg.
                
                Det er gjort unntak for opphold i barnevernsinstitusjoner. Dette kommer frem av tiltakspengeforskriften § 9.
            """.trimIndent()

        HjemmelForOpphør.IkkeLovligOpphold ->
            """
                I denne perioden har du ikke lovlig opphold i Norge.
                
                Du må ha lovlig opphold i Norge, for å ha rett til $tiltakspengerOgKanskjeBarnetillegg.
                
                Dette kommer frem av arbeidsmarkedsloven § 2.
            """.trimIndent()

        // Saksbehandler må bruke fritekst for disse hjemlene
        HjemmelForOpphør.Alder,
        HjemmelForOpphør.FremmetForSent,
        -> null
    }
}
