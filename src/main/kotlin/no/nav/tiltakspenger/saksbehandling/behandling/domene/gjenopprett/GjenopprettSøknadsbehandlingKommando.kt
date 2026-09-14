package no.nav.tiltakspenger.saksbehandling.behandling.domene.gjenopprett

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler

/**
 * Saksbehandler peker på den avbrutte søknadsbehandlingen hen vil ta opp igjen.
 * Det er søknaden som gjenopprettes; den avbrutte behandlingen står urørt og erstattes av en ny.
 */
data class GjenopprettSøknadsbehandlingKommando(
    val sakId: SakId,
    val avbruttBehandlingId: RammebehandlingId,
    val saksbehandler: Saksbehandler,
    val begrunnelse: NonBlankString?,
    val correlationId: CorrelationId,
)
