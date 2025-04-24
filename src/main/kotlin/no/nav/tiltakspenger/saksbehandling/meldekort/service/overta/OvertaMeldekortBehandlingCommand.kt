package no.nav.tiltakspenger.saksbehandling.meldekort.service.overta

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.ServiceCommand

data class OvertaMeldekortBehandlingCommand(
    val sakId: SakId,
    val meldekortId: MeldekortId,
    val overtarFra: String,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
) : ServiceCommand
