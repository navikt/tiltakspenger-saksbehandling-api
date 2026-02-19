package no.nav.tiltakspenger.saksbehandling.klage.ports

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.OversendtKlageTilKabalMetadata

interface KlagebehandlingRepo {
    fun lagreKlagebehandling(klagebehandling: Klagebehandling, sessionContext: SessionContext? = null)

    fun hentForRammebehandlingId(rammebehandlingId: BehandlingId): Klagebehandling?

    /** Egen funksjon for at saksbehandlerne ikke skal gå i beina på hverandre. */
    fun taBehandling(klagebehandling: Klagebehandling, sessionContext: SessionContext?): Boolean

    /** Egen funksjon for at saksbehandlerne ikke skal gå i beina på hverandre. */
    fun overtaBehandling(
        klagebehandling: Klagebehandling,
        nåværendeSaksbehandler: String,
        sessionContext: SessionContext?,
    ): Boolean

    fun hentInnstillingsbrevSomSkalJournalføres(limit: Int = 10): List<Klagebehandling>
    fun markerInnstillingsbrevJournalført(klagebehandling: Klagebehandling, metadata: JournalførBrevMetadata)
    fun hentInnstillingsbrevSomSkalDistribueres(limit: Int = 10): List<Klagebehandling>

    fun hentSakerSomSkalOversendesKlageinstansen(limit: Int = 10): List<SakId>

    /** Egen funksjon for ikke å ha metadataene i [no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Opprettholdt] */
    fun markerOversendtTilKlageinstans(klagebehandling: Klagebehandling, metadata: OversendtKlageTilKabalMetadata)
}
