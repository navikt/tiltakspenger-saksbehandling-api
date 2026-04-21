package no.nav.tiltakspenger.saksbehandling.behandling.domene.settPåVent

import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import java.time.LocalDate
import java.time.LocalDateTime

data class SettRammebehandlingPåVentKommando(
    val sakId: SakId,
    val rammebehandlingId: RammebehandlingId,
    val begrunnelse: String,
    val saksbehandler: Saksbehandler,
    val venterTil: LocalDateTime? = null,
    val frist: LocalDate?,
)
