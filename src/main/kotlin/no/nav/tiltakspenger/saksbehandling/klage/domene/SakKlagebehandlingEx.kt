package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
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

fun Sak.åpneRammebehandlingerMedKlagebehandlingId(klagebehandlingId: KlagebehandlingId): List<Rammebehandling> {
    return this.behandlinger.hentÅpneRammebehandlingerMedKlagebehandlingId(klagebehandlingId)
}

fun Sak.hentKlagebehandlingerSomSkalOversendesKlageinstansen(): List<Klagebehandling> {
    return this.behandlinger.hentKlagebehandlingerSomSkalOversendesKlageinstansen()
}

fun Sak.hentJournalpostIdForVedtakId(vedtakId: VedtakId): JournalpostId {
    return this.vedtaksliste.hentJournalpostIdForVedtakId(vedtakId)
}
