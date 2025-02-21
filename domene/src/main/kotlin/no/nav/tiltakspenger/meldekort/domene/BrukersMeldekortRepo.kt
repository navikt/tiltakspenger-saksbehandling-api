package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext

interface BrukersMeldekortRepo {
    fun lagre(
        brukersMeldekort: NyttBrukersMeldekort,
        sessionContext: SessionContext? = null,
    )

    fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): List<BrukersMeldekort>

    fun hentForMeldekortId(
        meldekortId: MeldekortId,
        sessionContext: SessionContext? = null,
    ): BrukersMeldekort?

    fun hentForMeldeperiodeId(
        hendelseId: HendelseId,
        sessionContext: SessionContext? = null,
    ): BrukersMeldekort?
}
