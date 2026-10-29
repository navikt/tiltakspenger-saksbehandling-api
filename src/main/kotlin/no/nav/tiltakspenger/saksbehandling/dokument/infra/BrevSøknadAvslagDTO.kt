package no.nav.tiltakspenger.saksbehandling.dokument.infra

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Hjemmel
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
    override val datoForUtsending: LocalDate,
    override val tilleggstekst: String?,
    override val forhandsvisning: Boolean,
    val avslagsgrunner: List<GrunnBrevDto>,
    val harSøktMedBarn: Boolean,
    /**
     * Hjemlene avslaget bygger på, også når det bare har én grunn.
     * pdfgenrs bruker dem til å lage setningen med de samlede hjemlene når avslaget har flere grunner.
     */
    val hjemler: List<HjemmelBrevDto>,
    val avslagFraOgMed: LocalDate,
    val avslagTilOgMed: LocalDate,
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
        avslagsgrunner = avslagsgrunner.map { it.tilGrunnBrevDto() },
        hjemler = avslagsgrunner.hjemlerForBrev(harSøktBarnetillegg).map { it.tilHjemmelBrevDto() },
        harSøktMedBarn = harSøktBarnetillegg,
        saksbehandlerNavn = saksbehandlersNavn,
        beslutterNavn = besluttersNavn,
        avslagFraOgMed = avslagsperiode.fraOgMed,
        avslagTilOgMed = avslagsperiode.tilOgMed,
        datoForUtsending = datoForUtsending,
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
 * Hjemlene brevet viser til: alle hjemlene til grunnene, pluss § 3 i tiltakspengeforskriften når det er søkt om barnetillegg.
 */
private fun Set<Avslagsgrunnlag>.hjemlerForBrev(harSøktBarnetillegg: Boolean): List<Hjemmel> =
    this.flatMap { it.hjemler } +
        if (harSøktBarnetillegg) listOf(Hjemmel.TiltakspengeforskriftenHjemmel.TILTAKSPENGEFORSKRIFTEN_3) else emptyList()
