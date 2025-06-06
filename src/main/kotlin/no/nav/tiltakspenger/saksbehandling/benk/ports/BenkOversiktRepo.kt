package no.nav.tiltakspenger.saksbehandling.benk.ports

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversikt
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentÅpneBehandlingerCommand

/**
 * Custom spørringer for å vise en oversikt over søknader og behandlinger.
 */
interface BenkOversiktRepo {
    fun hentÅpneBehandlinger(command: HentÅpneBehandlingerCommand, sessionContext: SessionContext? = null, limit: Int = 500): BenkOversikt
}
