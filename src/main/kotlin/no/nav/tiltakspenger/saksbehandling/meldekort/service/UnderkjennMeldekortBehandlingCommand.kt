package no.nav.tiltakspenger.saksbehandling.meldekort.service

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.ServiceCommand

data class UnderkjennMeldekortBehandlingCommand(
    val meldekortId: MeldekortId,
    val begrunnelse: String,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
) : ServiceCommand
