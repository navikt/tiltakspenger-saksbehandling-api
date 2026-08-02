package no.nav.tiltakspenger.saksbehandling.klage.ports

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.OversendtKlageTilKabalMetadata

interface KlagebehandlingRepo {
    fun lagreKlagebehandling(klagebehandling: Klagebehandling, sessionContext: SessionContext? = null)

    fun hentForKlagebehandlingId(klagebehandlingId: KlagebehandlingId): Klagebehandling?

    fun hentInnstillingsbrevSomSkalJournalføres(limit: Int = 10): List<Klagebehandling>

    fun markerInnstillingsbrevJournalført(klagebehandling: Klagebehandling, metadata: JournalførBrevMetadata)

    fun hentInnstillingsbrevSomSkalDistribueres(limit: Int = 10): List<Klagebehandling>

    fun hentSakerSomSkalOversendesKlageinstansen(limit: Int = 10): List<SakId>

    /** Egen funksjon for ikke å ha metadataene i [no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Opprettholdt] */
    fun markerOversendtTilKlageinstans(
        klagebehandling: Klagebehandling,
        metadata: OversendtKlageTilKabalMetadata,
        sessionContext: SessionContext?,
    )
}
