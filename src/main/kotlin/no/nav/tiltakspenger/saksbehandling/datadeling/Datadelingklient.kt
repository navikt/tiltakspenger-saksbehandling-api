package no.nav.tiltakspenger.saksbehandling.datadeling

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling.Behandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.vedtak.Rammevedtak

interface DatadelingClient {

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
