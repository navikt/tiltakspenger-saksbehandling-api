package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlinger
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.oppretthold.OversendtKlageTilKabalMetadata

class KlagebehandlingFakeRepo : KlagebehandlingRepo {

    private val data = Atomic(mutableMapOf<KlagebehandlingId, Klagebehandling>())
    val alle get() = data.get().values.toList()

    override fun lagreKlagebehandling(
        klagebehandling: Klagebehandling,
        sessionContext: SessionContext?,
    ) {
        data.get()[klagebehandling.id] = klagebehandling
    }

    override fun hentForKlagebehandlingId(klagebehandlingId: KlagebehandlingId): Klagebehandling? {
        return data.get()[klagebehandlingId]
    }

    override fun hentInnstillingsbrevSomSkalJournalføres(limit: Int): List<Klagebehandling> {
        return data.get().values.filter {
            it.status == Klagebehandlingsstatus.OPPRETTHOLDT &&
                it.resultat is Klagebehandlingsresultat.Opprettholdt &&
                it.resultat.journalpostIdInnstillingsbrev == null
        }.take(limit)
    }

    override fun markerInnstillingsbrevJournalført(
        klagebehandling: Klagebehandling,
        metadata: JournalførBrevMetadata,
    ) {
        data.get()[klagebehandling.id] = klagebehandling
    }

    override fun hentInnstillingsbrevSomSkalDistribueres(limit: Int): List<Klagebehandling> {
        return data.get().values.filter {
            it.status == Klagebehandlingsstatus.OPPRETTHOLDT &&
                it.resultat is Klagebehandlingsresultat.Opprettholdt &&
                it.resultat.journalpostIdInnstillingsbrev != null &&
                it.resultat.distribusjonIdInnstillingsbrev == null
        }.take(limit)
    }

    override fun hentSakerSomSkalOversendesKlageinstansen(limit: Int): List<SakId> {
        return data.get().values.filter { it.kanOversendeKlageinstans }.map { it.sakId }.take(limit)
    }

    override fun markerOversendtTilKlageinstans(
        klagebehandling: Klagebehandling,
        metadata: OversendtKlageTilKabalMetadata,
        sessionContext: SessionContext?,
    ) {
        data.get()[klagebehandling.id] = klagebehandling
    }

    fun hentForSakId(sakId: SakId): Klagebehandlinger {
        return Klagebehandlinger(data.get().values.filter { it.sakId == sakId })
    }
}
