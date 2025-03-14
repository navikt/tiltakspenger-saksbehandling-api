package no.nav.tiltakspenger.saksbehandling.clients.pdfgen

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.clients.pdfgen.formattering.norskDatoFormatter
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.ValgtHjemmelHarIkkeRettighet
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.personopplysninger.Navn
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vedtak.Rammevedtak
import java.time.LocalDate

@Suppress("unused")
private class BrevRevurderingStansDTO(
    val personalia: BrevPersonaliaDTO,
    val saksnummer: String,
    val datoForUtsending: String,
    val barnetillegg: Boolean = false,
    @Deprecated("rammevedtakFraDato er renamet til virkningsperiodeFraDato, beholdes frem til pdfgen har fjernet bruken")
    val rammevedtakFraDato: String,
    @Deprecated("rammevedtakTilDato er renamet til virkvirkningsperiodeTilDatoningsperiodeFraDato, beholdes til pdfgen har fjernet bruken")
    val rammevedtakTilDato: String,
    val virkningsperiodeFraDato: String,
    val virkningsperiodeTilDato: String,
    val kontor: String,
    val beslutterNavn: String?,
    val saksbehandlerNavn: String,
    val forhandsvisning: Boolean,
    val tilleggstekst: String? = null,
    val valgtHjemmelTekst: String?,
)

internal suspend fun Rammevedtak.toRevurderingStans(
    hentBrukersNavn: suspend (Fnr) -> Navn,
    hentSaksbehandlersNavn: suspend (String) -> String,
    vedtaksdato: LocalDate,
): String {
    return genererStansbrev(
        hentBrukersNavn = hentBrukersNavn,
        hentSaksbehandlersNavn = hentSaksbehandlersNavn,
        vedtaksdato = vedtaksdato,
        fnr = fnr,
        saksbehandlerNavIdent = saksbehandlerNavIdent,
        beslutterNavIdent = beslutterNavIdent,
        virkningsperiode = this.periode,
        saksnummer = saksnummer,
        // finnes ikke noe forhåndsvisning for Rammevedtak
        forhåndsvisning = false,
        barnetillegg = barnetillegg != null,
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
    valgtHjemmel: ValgtHjemmelHarIkkeRettighet? = null,
    tilleggstekst: String? = null,
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
        kontor = "Nav Tiltak Øst-Viken",
        // Dette er vår dato, det brukes typisk når bruker klager på vedtaksbrev på dato ...
        datoForUtsending = vedtaksdato.format(norskDatoFormatter),
        forhandsvisning = forhåndsvisning,
        valgtHjemmelTekst = valgtHjemmel?.tekstVedtaksbrev(barnetillegg),
        tilleggstekst = tilleggstekst,
    ).let { serialize(it) }
}

private fun ValgtHjemmelHarIkkeRettighet.tekstVedtaksbrev(barnetillegg: Boolean): String {
    val ogBarnetillegg = "{og barnetillegg}"
    val barnetilleggTekst = if (barnetillegg) " og barnetillegg" else ""

    val tekstVedtaksbrev = when (this.kode) {
        ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak.kode ->
            "du ikke lenger deltar på tiltak. Deltakere som ikke deltar på tiltak, har ikke rett til tiltakspenger $ogBarnetillegg etter tiltakspengeforskriften §2."

        ValgtHjemmelForStans.Alder.kode ->
            // TODO - få inn korrekt tekst når den er klar https://confluence.adeo.no/x/qAJ7K
            "du ikke oppfyller tiltakspengeforskriften § 3 - alder"

        ValgtHjemmelForStans.Livsoppholdytelser.kode ->
            "du mottar en annen stønad til livsopphold. Deltakere som mottar andre stønader til livsopphold, har ikke rett til tiltakspenger $ogBarnetillegg etter forskrift om tiltakspenger § 7."

        ValgtHjemmelForStans.Kvalifiseringsprogrammet.kode ->
            "du deltar på kvalifiseringsprogram. Deltakere i kvalifiseringsprogram, har ikke rett til tiltakspenger $ogBarnetillegg etter forskrift om tiltakspenger § 7, tredje ledd."

        ValgtHjemmelForStans.Introduksjonsprogrammet.kode ->
            "du deltar på introduksjonsprogram. Deltakere i introduksjonsprogram, har ikke rett til tiltakspenger $ogBarnetillegg etter forskrift om tiltakspenger § 7, tredje ledd."

        ValgtHjemmelForStans.LønnFraTiltaksarrangør.kode ->
            // TODO - få inn korrekt tekst når den er klar https://confluence.adeo.no/x/qAJ7K
            "du ikke oppfyller tiltakspengeforskriften § 8 - lønn fra tiltaksarrangør"

        ValgtHjemmelForStans.LønnFraAndre.kode ->
            // TODO - få inn korrekt tekst når den er klar https://confluence.adeo.no/x/qAJ7K
            "du ikke oppfyller arbeidsmarkedsloven § 13 - lønn fra andre"

        ValgtHjemmelForStans.Institusjonsopphold.kode ->
            "du oppholder deg i institusjon med fri kost og losji. Deltakere som har opphold i institusjon, fengsel mv. med fri kost og losji under gjennomføringen av tiltaket, kan ikke samtidig motta tiltakspenger etter tiltakspengeforskriften §9 "

        ValgtHjemmelForStans.Annet.kode ->
            // TODO - få inn korrekt tekst når den er klar https://confluence.adeo.no/x/qAJ7K
            "du oppfyller ikke kriteriene for å få tiltakspenger. Se avsnittet \"Slik har vi vurdert saken din\" for mer informasjon."

        else -> {
            throw IllegalStateException("Ukjent valgt hjemmel: $this")
        }
    }

    return tekstVedtaksbrev.replace(" $ogBarnetillegg", barnetilleggTekst)
}
