package no.nav.tiltakspenger.saksbehandling.benk.service

import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag
import no.nav.tiltakspenger.saksbehandling.benk.ports.BenkOversiktRepo

data class TilgangsfiltrertBenkOversikt(
    val behandlingssammendrag: List<Behandlingssammendrag>,
    val totalAntall: Int,
    val totalAntallUfiltrert: Int,
    val antallFiltrertPgaTilgang: Int,
) {
    val limit = BenkOversiktRepo.DEFAULT_LIMIT

    companion object {
        fun empty(totalAntallUfiltrert: Int) = TilgangsfiltrertBenkOversikt(
            behandlingssammendrag = emptyList(),
            totalAntall = 0,
            totalAntallUfiltrert = totalAntallUfiltrert,
            antallFiltrertPgaTilgang = 0,
        )
    }
}
