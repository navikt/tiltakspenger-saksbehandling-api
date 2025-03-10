package no.nav.tiltakspenger.vedtak.datadeling.ports

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.behandling.Behandling
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.vedtak.Rammevedtak

interface DatadelingGateway {
    suspend fun send(
        rammevedtak: Rammevedtak,
        correlationId: CorrelationId,
    ): Either<FeilVedSendingTilDatadeling, Unit>
    suspend fun send(
        behandling: Behandling,
        correlationId: CorrelationId,
    ): Either<FeilVedSendingTilDatadeling, Unit>
}

data object FeilVedSendingTilDatadeling
