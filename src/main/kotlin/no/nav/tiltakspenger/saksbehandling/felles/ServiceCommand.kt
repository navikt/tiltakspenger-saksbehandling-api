package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler

interface ServiceCommand {
    val saksbehandler: Saksbehandler
    val correlationId: CorrelationId
}
