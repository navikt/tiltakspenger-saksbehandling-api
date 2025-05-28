package no.nav.tiltakspenger.saksbehandling.benk.infra.routes

data class BehandlingssammendragDTO(
    val fnr: String,
    val saksnummer: String,
    val startet: String,
    val behandlingstype: BehandlingssammendragTypeDTO,
    val status: BehandlingssammendragStatusDto?,
    val saksbehandler: String?,
    val beslutter: String?,
)

enum class BehandlingssammendragTypeDTO {
    SØKNADSBEHANDLING,
    REVURDERING,
    MELDEKORTBEHANDLING,
}
