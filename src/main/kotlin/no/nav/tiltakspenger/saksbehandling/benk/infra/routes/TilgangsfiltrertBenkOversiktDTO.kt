package no.nav.tiltakspenger.saksbehandling.benk.infra.routes

data class TilgangsfiltrertBenkOversiktDTO(
    val behandlingssammendrag: List<BehandlingssammendragDTO>,
    val totalAntall: Int,
    val antallFiltrertPgaTilgang: Int,
)
