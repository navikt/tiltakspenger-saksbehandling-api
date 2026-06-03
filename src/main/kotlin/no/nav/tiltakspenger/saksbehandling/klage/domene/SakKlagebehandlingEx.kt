package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AttesterbarBehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak

fun Sak.leggTilKlagebehandling(klagebehandling: Klagebehandling): Sak {
    return this.copy(behandlinger = this.behandlinger.leggTilKlagebehandling(klagebehandling))
}

fun Sak.oppdaterKlagebehandling(klagebehandling: Klagebehandling): Sak {
    return this.copy(behandlinger = this.behandlinger.oppdaterKlagebehandling(klagebehandling))
}

fun Sak.hentKlagebehandling(klagebehandlingId: KlagebehandlingId): Klagebehandling {
    return this.behandlinger.hentKlagebehandling(klagebehandlingId)
}

fun Sak.åpneBehandlingerMedKlagebehandlingId(klagebehandlingId: KlagebehandlingId): List<AttesterbarBehandling> {
    return this.behandlinger.hentÅpneBehandlingerMedKlagebehandlingId(klagebehandlingId)
}

/**
 * Behandling er ikke sealed, noe som vil gjøre det vanskelig å holde styr på ulike behandlings-handlinger (vent, gjenoppta, legg tilbake, etc)
 * hvor det også skal inngå klagen.
 *
 * Dette er bare en ekstra wrapper for å få til sealed funksjonaliteten, og gjøre det enklere å finne hvor ting er brukt.
 */
sealed interface AktivTilknyttetBehandling {
    val behandling: AttesterbarBehandling

    data class Ramme(val rammebehandling: Rammebehandling) : AktivTilknyttetBehandling {
        override val behandling: AttesterbarBehandling = rammebehandling
    }

    data class Meldekort(val meldekortbehandling: Meldekortbehandling) : AktivTilknyttetBehandling {
        override val behandling: AttesterbarBehandling = meldekortbehandling
    }
}

fun Sak.hentAktivTilknyttetBehandling(klagebehandling: Klagebehandling): AktivTilknyttetBehandling? {
    return klagebehandling.behandlingId.mapNotNull { behandlingId ->
        when (behandlingId) {
            is RammebehandlingId -> this.hentRammebehandling(behandlingId)
                ?.takeIf { it.erUnderAktivBehandling }
                ?.let { AktivTilknyttetBehandling.Ramme(it) }

            is MeldekortId -> this.hentMeldekortbehandling(behandlingId)
                ?.takeIf { it.erÅpen() }
                ?.let { AktivTilknyttetBehandling.Meldekort(it) }

            else -> null
        }
    }.singleOrNullOrThrow()
}

fun Sak.hentTilknyttedeBehandlinger(klagebehandling: Klagebehandling): List<AttesterbarBehandling> {
    return klagebehandling.behandlingId.mapNotNull { behandlingId ->
        when (behandlingId) {
            is RammebehandlingId -> this.hentRammebehandling(behandlingId)
            is MeldekortId -> this.hentMeldekortbehandling(behandlingId)
            else -> null
        }
    }
}

fun Sak.hentKlagebehandlingerSomSkalOversendesKlageinstansen(): List<Klagebehandling> {
    return this.behandlinger.hentKlagebehandlingerSomSkalOversendesKlageinstansen()
}

fun Sak.hentJournalpostIdForVedtakId(vedtakId: VedtakId): JournalpostId {
    return this.vedtaksliste.hentJournalpostIdForVedtakId(vedtakId)
}
