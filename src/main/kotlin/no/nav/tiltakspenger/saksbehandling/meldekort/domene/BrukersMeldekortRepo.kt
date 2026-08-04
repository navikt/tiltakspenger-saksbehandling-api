package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatiskStatus

interface BrukersMeldekortRepo {
    fun lagre(
        brukersMeldekort: BrukersMeldekort,
        sessionContext: SessionContext? = null,
    )

    fun hentForMeldekortId(
        meldekortId: MeldekortId,
        sessionContext: SessionContext? = null,
    ): BrukersMeldekort?

    fun hentMeldekortSomSkalBehandlesAutomatisk(
        limit: Int = 100,
        sessionContext: SessionContext? = null,
    ): List<BrukersMeldekort>

    fun oppdaterAutomatiskBehandletStatus(
        meldekortId: MeldekortId,
        status: MeldekortBehandletAutomatiskStatus,
        behandlesAutomatisk: Boolean,
        metadata: Forsøkshistorikk,
        sessionContext: SessionContext? = null,
    )

    fun markerSomAutomatiskBehandlet(
        meldekortId: MeldekortId,
        metadata: Forsøkshistorikk,
        sessionContext: SessionContext?,
    )
}
