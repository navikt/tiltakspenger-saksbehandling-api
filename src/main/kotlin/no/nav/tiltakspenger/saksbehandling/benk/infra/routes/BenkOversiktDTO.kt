package no.nav.tiltakspenger.saksbehandling.benk.infra.routes

data class BenkOversiktDTO(
    val behandlingssammendrag: List<BehandlingssammendragDTO>,
    val totalAntall: Int,
)
