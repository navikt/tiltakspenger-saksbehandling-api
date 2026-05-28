package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import no.nav.tiltakspenger.libs.common.Fnr

class NavkontorService(
    private val veilarboppfolgingKlient: VeilarboppfolgingKlient,
) {
    /**
     * Returnerer [Navkontor] for bakoverkompatibilitet. Klienten returnerer rik metadata
     * ([NavkontorMedMetadata]) som vi senere vil ønske å lagre - se [hentOppfolgingsenhetMedMetadata].
     * Kaster [IllegalStateException] dersom klienten ikke klarte å hente navkontor.
     */
    suspend fun hentOppfolgingsenhet(
        fnr: Fnr,
        sakId: String? = null,
        saksnummer: String? = null,
        rammebehandlingId: String? = null,
        meldekortbehandlingId: String? = null,
    ): Navkontor = hentOppfolgingsenhetMedMetadata(
        fnr = fnr,
        sakId = sakId,
        saksnummer = saksnummer,
        rammebehandlingId = rammebehandlingId,
        meldekortbehandlingId = meldekortbehandlingId,
    ).fold(
        ifLeft = { error("Kunne ikke hente navkontor: $it") },
        ifRight = { it.navkontor },
    )

    suspend fun hentOppfolgingsenhetMedMetadata(
        fnr: Fnr,
        sakId: String? = null,
        saksnummer: String? = null,
        rammebehandlingId: String? = null,
        meldekortbehandlingId: String? = null,
    ) = veilarboppfolgingKlient.hentOppfolgingsenhet(
        fnr = fnr,
        sakId = sakId,
        saksnummer = saksnummer,
        rammebehandlingId = rammebehandlingId,
        meldekortbehandlingId = meldekortbehandlingId,
    )
}
