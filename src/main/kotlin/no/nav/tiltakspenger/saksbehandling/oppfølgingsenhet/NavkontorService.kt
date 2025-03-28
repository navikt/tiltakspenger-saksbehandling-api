package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import no.nav.tiltakspenger.libs.common.Fnr

class NavkontorService(
    private val veilarboppfolgingGateway: VeilarboppfolgingGateway,
) {
    suspend fun hentOppfolgingsenhet(fnr: Fnr): Navkontor {
        return veilarboppfolgingGateway.hentOppfolgingsenhet(fnr)
    }
}
