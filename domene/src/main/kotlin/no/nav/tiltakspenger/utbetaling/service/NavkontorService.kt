package no.nav.tiltakspenger.utbetaling.service

import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.ports.VeilarboppfolgingGateway

class NavkontorService(
    private val veilarboppfolgingGateway: VeilarboppfolgingGateway,
) {
    suspend fun hentOppfolgingsenhet(fnr: Fnr): Navkontor {
        return veilarboppfolgingGateway.hentOppfolgingsenhet(fnr.verdi)
    }
}
