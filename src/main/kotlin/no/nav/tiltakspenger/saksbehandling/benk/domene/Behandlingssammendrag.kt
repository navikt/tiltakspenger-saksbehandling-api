package no.nav.tiltakspenger.saksbehandling.benk.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDateTime

data class Behandlingssammendrag(
    val sakId: SakId,
    val fnr: Fnr,
    val saksnummer: Saksnummer,
    val startet: LocalDateTime,
    /**
     * Dette fylles bare ut for søknader og søknadsbehandlinger
     * For alle andre er de null.
     * [kravtidspunkt] er egentlig det samme som [startet]
     */
    val kravtidspunkt: LocalDateTime?,
    val behandlingstype: BehandlingssammendragType,
    val status: BehandlingssammendragStatus?,
    val saksbehandler: String?,
    val beslutter: String?,
) {
    init {
        if (behandlingstype == BehandlingssammendragType.SØKNADSBEHANDLING) {
            require(kravtidspunkt != null) {
                "Kravtidspunkt må være satt for søknadsbehandlinger"
            }
        }
        if (behandlingstype != BehandlingssammendragType.SØKNADSBEHANDLING) {
            require(kravtidspunkt == null) {
                "Kravtidspunkt skal være null for ikke-søknadsbehandlinger"
            }
        }
    }
}
