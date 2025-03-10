package no.nav.tiltakspenger.vedtak.utbetaling.service

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.vedtak.felles.Navkontor
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.VeilarboppfolgingGateway

class NavkontorService(
    private val veilarboppfolgingGateway: VeilarboppfolgingGateway,
) {
    suspend fun hentOppfolgingsenhet(fnr: Fnr): Navkontor {
        return veilarboppfolgingGateway.hentOppfolgingsenhet(fnr)
    }
}
