package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import no.nav.tiltakspenger.libs.common.Fnr

interface VeilarboppfolgingKlient {
    suspend fun hentOppfolgingsenhet(
        fnr: Fnr,
        sakId: String? = null,
        saksnummer: String? = null,
        rammebehandlingId: String? = null,
        meldekortbehandlingId: String? = null,
    ): Navkontor
}
