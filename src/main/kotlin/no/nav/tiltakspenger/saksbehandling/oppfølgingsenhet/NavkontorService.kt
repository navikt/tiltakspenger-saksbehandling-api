package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import no.nav.tiltakspenger.libs.common.Fnr

class NavkontorService(
    private val veilarboppfolgingKlient: VeilarboppfolgingKlient,
) {
    suspend fun hentOppfolgingsenhet(
        fnr: Fnr,
        sakId: String? = null,
        saksnummer: String? = null,
        rammebehandlingId: String? = null,
        meldekortbehandlingId: String? = null,
    ): Navkontor =
        veilarboppfolgingKlient.hentOppfolgingsenhet(
            fnr = fnr,
            sakId = sakId,
            saksnummer = saksnummer,
            rammebehandlingId = rammebehandlingId,
            meldekortbehandlingId = meldekortbehandlingId,
        )
}
