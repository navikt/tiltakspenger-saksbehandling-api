package no.nav.tiltakspenger.saksbehandling.klage.domene.leggTilbake

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId

data class LeggTilbakeKlagebehandlingKommando(
    val sakId: SakId,
    val klagebehandlingId: KlagebehandlingId,
    val saksbehandler: Saksbehandler,
)
