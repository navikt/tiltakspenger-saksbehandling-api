package no.nav.tiltakspenger.vedtak.routes.exceptionhandling

import no.nav.tiltakspenger.libs.ktor.common.ErrorJson

object Standardfeil {

    fun fantIkkeFnr(): ErrorJson = ErrorJson(
        "Fant ikke fødselsnummer",
        "fant_ikke_fnr",
    )

    fun fantIkkeSak(): ErrorJson = ErrorJson(
        "Fant ikke sak",
        "fant_ikke_sak",
    )

    fun fantIkkeMeldekort(): ErrorJson = ErrorJson(
        "Fant ikke meldekort",
        "fant_ikke_meldekort",
    )

    fun fantIkkeTiltak(): ErrorJson = ErrorJson(
        "Fant ikke igjen tiltaket det er søkt på",
        "fant_ikke_tiltak",
    )

    fun måVæreBeslutter(): ErrorJson = ErrorJson(
        "Må ha beslutter-rolle.",
        "må_ha_beslutter_rolle",
    )

    fun måVæreSaksbehandlerEllerBeslutter(): ErrorJson = ErrorJson(
        "Må være eller saksbehandler eller beslutter",
        "må_være_beslutter_eller_saksbehandler",
    )

    fun saksopplysningsperiodeMåVæreLik(): ErrorJson = ErrorJson(
        "Perioden til saksopplysningen er forskjellig fra vurderingsperioden",
        "saksopplysningsperiode_må_være_lik",
    )

    fun måVæreSaksbehandler(): ErrorJson = ErrorJson(
        "Må ha saksbehandler-rolle.",
        "må_ha_saksbehandler_rolle",
    )

    fun støtterIkkeDelvisEllerAvslag(): ErrorJson = ErrorJson(
        "Vi støtter ikke delvis innvilgelse eller avslag.",
        "støtter_ikke_delvis_innvilgelse_eller_avslag",
    )

    fun saksbehandlerOgBeslutterKanIkkeVæreLik(): ErrorJson = ErrorJson(
        "Beslutter kan ikke være den samme som saksbehandler.",
        "beslutter_og_saksbehandler_kan_ikke_være_lik",
    )

    fun ikkeImplementert(): ErrorJson = ErrorJson(
        "Vi mangler en implementasjon for å gjennomføre denne operasjonen",
        "ikke_implementert",
    )

    fun ikkeAutorisert(): ErrorJson = ErrorJson(
        "Kunne ikke autorisere bruker",
        "ikke_autorisert",
    )

    fun serverfeil(): ErrorJson = ErrorJson(
        "Noe gikk galt på serversiden",
        "server_feil",
    )

    fun ugyldigRequest(): ErrorJson = ErrorJson(
        "Kunne ikke prosessere request",
        "ugyldig_request",
    )

    fun ikkeTilgang(
        melding: String = "Bruker har ikke tilgang",
    ): ErrorJson = ErrorJson(
        melding,
        "ikke_tilgang",
    )

    fun ikkeFunnet(): ErrorJson = ErrorJson(
        "Fant ikke ressursen",
        "ikke_funnet",
    )
}
