package no.nav.tiltakspenger.saksbehandling.infra.route

import no.nav.tiltakspenger.libs.ktor.common.ErrorJson

object Standardfeil {

    fun fantIkkeFnr(): ErrorJson = ErrorJson(
        "Fant ikke fødselsnummer",
        "fant_ikke_fnr",
    )

    fun saksbehandlerOgBeslutterKanIkkeVæreLik(): ErrorJson = ErrorJson(
        "Beslutter kan ikke være den samme som saksbehandler.",
        "beslutter_og_saksbehandler_kan_ikke_være_lik",
    )

    fun behandlingenEiesAvAnnenSaksbehandler(eiesAvSaksbehandler: String?) = ErrorJson(
        "Du kan ikke utføre handlinger på en behandling som ikke er tildelt deg. Behandlingen er tildelt $eiesAvSaksbehandler",
        "behandling_eies_av_annen_saksbehandler",
    )

    fun kanIkkeOppdatereBehandling() = ErrorJson(
        "Kan ikke oppdatere behandling i nåværende tilstand",
        "kan_ikke_oppdatere_behandling",
    )

    fun serverfeil(): ErrorJson = ErrorJson(
        "Noe gikk galt på serversiden",
        "server_feil",
    )

    fun ugyldigRequest(): ErrorJson = ErrorJson(
        "Kunne ikke prosessere request",
        "ugyldig_request",
    )

    fun ugyldigJournalpostIdInput(melding: String): ErrorJson = ErrorJson(
        melding,
        "ugyldig_jp_input",
    )

    fun ikkeFunnet(): ErrorJson = ErrorJson(
        "Fant ikke ressursen",
        "ikke_funnet",
    )
}
