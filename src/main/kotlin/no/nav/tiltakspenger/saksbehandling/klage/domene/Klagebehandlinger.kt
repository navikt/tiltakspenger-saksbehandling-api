package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.singleOrNullOrThrow
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

data class Klagebehandlinger(
    val klagebehandlinger: List<Klagebehandling>,
) : List<Klagebehandling> by klagebehandlinger {
    val fnr: Fnr? = klagebehandlinger.map { it.fnr }.distinct().singleOrNullOrThrow()
    val sakId: SakId? = klagebehandlinger.map { it.sakId }.distinct().singleOrNullOrThrow()
    val saksnummer: Saksnummer? = klagebehandlinger.map { it.saksnummer }.distinct().singleOrNullOrThrow()

    init {
        // opprettet rekkefølge
        // distinct id
    }
}
