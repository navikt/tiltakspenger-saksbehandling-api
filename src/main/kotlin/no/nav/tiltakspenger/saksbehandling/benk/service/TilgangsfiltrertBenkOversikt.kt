package no.nav.tiltakspenger.saksbehandling.benk.service

import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag

data class TilgangsfiltrertBenkOversikt(
    val behandlingssammendrag: List<Behandlingssammendrag>,
    val totalAntall: Int,
    val antallFiltrertPgaTilgang: Int,
) {

    companion object {
        fun empty() = TilgangsfiltrertBenkOversikt(
            behandlingssammendrag = emptyList(),
            totalAntall = 0,
            antallFiltrertPgaTilgang = 0,
        )
    }
}
