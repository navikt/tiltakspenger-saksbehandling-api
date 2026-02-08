package no.nav.tiltakspenger.saksbehandling.behandling.domene.overta

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.ServiceCommand

data class OvertaRammebehandlingKommando(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val overtarFra: String,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
) : ServiceCommand
