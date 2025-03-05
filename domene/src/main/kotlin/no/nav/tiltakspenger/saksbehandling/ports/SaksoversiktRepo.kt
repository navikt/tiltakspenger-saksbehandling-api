package no.nav.tiltakspenger.saksbehandling.ports

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.domene.benk.BehandlingEllerSøknadForSaksoversikt

/**
 * Custom spørringer for å vise en oversikt over søknader og behandlinger.
 */
interface SaksoversiktRepo {
    fun hentÅpneBehandlinger(sessionContext: SessionContext? = null): List<BehandlingEllerSøknadForSaksoversikt>
    fun hentÅpneSøknader(sessionContext: SessionContext? = null): List<BehandlingEllerSøknadForSaksoversikt>
}
