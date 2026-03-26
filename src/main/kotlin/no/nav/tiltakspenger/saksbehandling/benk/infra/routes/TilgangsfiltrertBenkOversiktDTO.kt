package no.nav.tiltakspenger.saksbehandling.benk.infra.routes

data class TilgangsfiltrertBenkOversiktDTO(
    val behandlingssammendrag: List<BehandlingssammendragDTO>,
    val totalAntall: Int,
    val totalAntallUfiltrert: Int,
    val antallFiltrertPgaTilgang: Int,
    val limit: Int,
)
