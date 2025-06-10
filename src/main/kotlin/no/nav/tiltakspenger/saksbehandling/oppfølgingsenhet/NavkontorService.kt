package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import no.nav.tiltakspenger.libs.common.Fnr

class NavkontorService(
    private val veilarboppfolgingKlient: VeilarboppfolgingKlient,
) {
    suspend fun hentOppfolgingsenhet(fnr: Fnr): Navkontor {
        return veilarboppfolgingKlient.hentOppfolgingsenhet(fnr)
    }
}
