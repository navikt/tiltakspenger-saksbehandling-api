package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.sak.Sak

fun Sak.leggTilKlagevedtak(klagevedtak: Klagevedtak): Sak {
    return this.copy(vedtaksliste = this.vedtaksliste.leggTilKlagevedtak(klagevedtak))
}

@Suppress("unused")
fun Sak.hentKlagevedtak(klagevedtakId: VedtakId): Klagevedtak {
    return this.vedtaksliste.klagevedtaksliste.hentForKlagevedtakId(klagevedtakId)
}

fun Sak.hentKlagevedtakForKlagebehandlingId(klagebehandlingId: KlagebehandlingId): Klagevedtak {
    return this.vedtaksliste.klagevedtaksliste.hentForKlagebehandlingId(klagebehandlingId)
}
