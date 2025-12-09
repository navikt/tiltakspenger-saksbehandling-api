package no.nav.tiltakspenger.saksbehandling.datadeling

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakDb
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.Clock

interface DatadelingClient {

    suspend fun send(
        rammevedtak: Rammevedtak,
        correlationId: CorrelationId,
    ): Either<FeilVedSendingTilDatadeling, Unit>

    suspend fun send(
        behandling: Behandling,
        correlationId: CorrelationId,
    ): Either<FeilVedSendingTilDatadeling, Unit>

    suspend fun send(
        meldeperioder: List<Meldeperiode>,
        correlationId: CorrelationId,
    ): Either<FeilVedSendingTilDatadeling, Unit>

    suspend fun send(
        godkjentMeldekort: MeldekortBehandling.Behandlet,
        clock: Clock,
        correlationId: CorrelationId,
    ): Either<FeilVedSendingTilDatadeling, Unit>

    suspend fun send(
        sakDb: SakDb,
        correlationId: CorrelationId,
    ): Either<FeilVedSendingTilDatadeling, Unit>
}

data object FeilVedSendingTilDatadeling
