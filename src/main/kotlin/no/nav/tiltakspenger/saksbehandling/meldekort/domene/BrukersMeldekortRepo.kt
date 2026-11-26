package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Prioritering
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling

interface BrukersMeldekortRepo {
    /** Et nytt meldekort er aldri prioritert, så [BrukersMeldekort.prioritering] lagres ikke. */
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

    /** @param prioritering Erstatter prioriteringen meldekortet har fra før. */
    fun oppdaterAutomatiskBehandletStatus(
        meldekortId: MeldekortId,
        status: MeldekortBehandletAutomatiskStatus,
        behandlesAutomatisk: Boolean,
        metadata: Forsøkshistorikk,
        prioritering: Prioritering?,
        sessionContext: SessionContext? = null,
    )

    fun markerSomAutomatiskBehandlet(
        meldekortId: MeldekortId,
        metadata: Forsøkshistorikk,
        sessionContext: SessionContext?,
    )

    /**
     * Lagrer [BrukersMeldekort.behandletAv] for meldekortene fra bruker som den iverksatte eller avbrutte [meldekortbehandling] behandlet.
     * Må kalles i samme transaksjon som iverksettelsen eller avbrytelsen.
     * Kaster dersom et av meldekortene allerede er behandlet, slik at transaksjonen rulles tilbake i stedet for å overskrive markeringen.
     */
    fun markerSomBehandlet(
        meldekortbehandling: Meldekortbehandling,
        sessionContext: SessionContext?,
    )
}
