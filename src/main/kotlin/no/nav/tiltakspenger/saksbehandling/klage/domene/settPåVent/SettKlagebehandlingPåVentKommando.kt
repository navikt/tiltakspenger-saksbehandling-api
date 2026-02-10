package no.nav.tiltakspenger.saksbehandling.klage.domene.settPåVent

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import java.time.LocalDate

data class SettKlagebehandlingPåVentKommando(
    val sakId: SakId,
    val klagebehandlingId: KlagebehandlingId,
    val saksbehandler: Saksbehandler,
    val begrunnelse: String,
    val frist: LocalDate?,
)
