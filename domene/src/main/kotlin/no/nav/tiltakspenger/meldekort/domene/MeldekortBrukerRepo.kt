package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext

interface MeldekortBrukerRepo {
    fun lagre(
        brukersMeldekort: BrukersMeldekort,
        sessionContext: SessionContext? = null,
    )
    fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): List<BrukersMeldekort>
}
