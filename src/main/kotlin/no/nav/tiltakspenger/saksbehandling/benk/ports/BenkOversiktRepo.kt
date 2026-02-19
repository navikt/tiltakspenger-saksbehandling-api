package no.nav.tiltakspenger.saksbehandling.benk.ports

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkOversikt
import no.nav.tiltakspenger.saksbehandling.benk.domene.HentÅpneBehandlingerCommand

/**
 * Custom spørringer for å vise en oversikt over søknader og behandlinger.
 */
interface BenkOversiktRepo {
    companion object {
        const val IKKE_TILDELT: String = "IKKE_TILDELT"
        const val DEFAULT_LIMIT = 500
    }

    fun hentÅpneBehandlinger(
        command: HentÅpneBehandlingerCommand,
        sessionContext: SessionContext? = null,
        limit: Int = DEFAULT_LIMIT,
    ): BenkOversikt
}
