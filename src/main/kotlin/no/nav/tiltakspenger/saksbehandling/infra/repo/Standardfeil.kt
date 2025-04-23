package no.nav.tiltakspenger.saksbehandling.infra.repo

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

    fun måVæreBeslutter(): ErrorJson = ErrorJson(
        "Må ha beslutter-rolle.",
        "må_ha_beslutter_rolle",
    )

    fun måVæreSaksbehandlerEllerBeslutter(): ErrorJson = ErrorJson(
        "Må være eller saksbehandler eller beslutter",
        "må_være_beslutter_eller_saksbehandler",
    )

    fun måVæreSaksbehandler(): ErrorJson = ErrorJson(
        "Må ha saksbehandler-rolle.",
        "må_ha_saksbehandler_rolle",
    )

    fun saksbehandlerOgBeslutterKanIkkeVæreLik(): ErrorJson = ErrorJson(
        "Beslutter kan ikke være den samme som saksbehandler.",
        "beslutter_og_saksbehandler_kan_ikke_være_lik",
    )

    fun ikkeImplementert(): ErrorJson = ErrorJson(
        "Vi mangler en implementasjon for å gjennomføre denne operasjonen",
        "ikke_implementert",
    )

    fun behandlingenEiesAvAnnenSaksbehandler(eiesAvSaksbehandler: String?) = ErrorJson(
        "Du kan ikke utføre handlinger på en behandling som ikke er tildelt deg. Behandlingen er tildelt $eiesAvSaksbehandler",
        "behandling_eies_av_annen_saksbehandler",
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
