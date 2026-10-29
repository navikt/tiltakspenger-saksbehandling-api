package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
import no.nav.tiltakspenger.saksbehandling.klage.domene.forvaltningsmelding.KlageForvaltningsmelding
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.OversendtKlageTilKabalMetadata

interface KlagebehandlingRepo {
    fun lagreKlagebehandling(klagebehandling: Klagebehandling, sessionContext: SessionContext? = null)

    fun hentForKlagebehandlingId(klagebehandlingId: KlagebehandlingId): Klagebehandling?

    fun hentInnstillingsbrevSomSkalJournalføres(limit: Int = 10): List<Klagebehandling>

    fun markerInnstillingsbrevJournalført(klagebehandling: Klagebehandling, metadata: JournalførBrevMetadata)

    fun hentInnstillingsbrevSomSkalDistribueres(limit: Int = 10): List<Klagebehandling>

    fun hentForvaltningsmeldingerSomSkalJournalføres(limit: Int = 10): List<Klagebehandling>

    fun markerForvaltningsmeldingJournalført(
        klagebehandlingId: KlagebehandlingId,
        forvaltningsmelding: KlageForvaltningsmelding,
        metadata: JournalførBrevMetadata,
    )

    fun hentForvaltningsmeldingerSomSkalDistribueres(limit: Int = 10): List<Klagebehandling>

    fun markerForvaltningsmeldingDistribuert(
        klagebehandlingId: KlagebehandlingId,
        forvaltningsmelding: KlageForvaltningsmelding,
    )

    fun hentSakerSomSkalOversendesKlageinstansen(limit: Int = 10): List<SakId>

    /** Egen funksjon for ikke å ha metadataene i [no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat.Opprettholdt] */
    fun markerOversendtTilKlageinstans(
        klagebehandling: Klagebehandling,
        metadata: OversendtKlageTilKabalMetadata,
        sessionContext: SessionContext?,
    )
}
