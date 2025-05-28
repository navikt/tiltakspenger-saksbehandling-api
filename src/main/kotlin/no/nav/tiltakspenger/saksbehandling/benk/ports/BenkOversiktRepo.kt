package no.nav.tiltakspenger.saksbehandling.benk.ports

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.benk.domene.Behandlingssammendrag

/**
 * Custom spørringer for å vise en oversikt over søknader og behandlinger.
 */
interface BenkOversiktRepo {
    fun hentÅpneBehandlinger(sessionContext: SessionContext? = null, limit: Int = 500): List<Behandlingssammendrag>
}
