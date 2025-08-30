package no.nav.tiltakspenger.saksbehandling.benk.infra.routes

data class BehandlingssammendragDTO(
    val sakId: String,
    val fnr: String,
    val saksnummer: String,
    val startet: String,
    val kravtidspunkt: String?,
    val behandlingstype: BehandlingssammendragTypeDTO,
    val status: BehandlingssammendragStatusDto?,
    val saksbehandler: String?,
    val beslutter: String?,
    val sistEndret: String?,
    val erSattPåVent: Boolean,
)

enum class BehandlingssammendragTypeDTO {
    SØKNADSBEHANDLING,
    REVURDERING,
    MELDEKORTBEHANDLING,
    INNSENDT_MELDEKORT,
    KORRIGERT_MELDEKORT,
}
